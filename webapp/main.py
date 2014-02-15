"""
	Telepromtu
	Google Glass Presentation Controller

	Requires:
		python >= 2.7
		python-flask
		google-appengine
"""

#############################################
# Dependencies
#############################################

import sys
sys.path.insert(0, 'lib')
from google.appengine.api import users
from google.appengine.api import channel
from google.appengine.api import memcache
from apiclient.discovery import build
from apiclient import errors
from pprint import pprint
from oauth2client.client import flow_from_clientsecrets
from oauth2client.client import FlowExchangeError
from oauth2client.client import AccessTokenRefreshError
from oauth2client.appengine import CredentialsProperty
from oauth2client.appengine import StorageByKeyName
from oauth2client.appengine import simplejson as json
from webapp2_extras import sessions
from gaesessions import get_current_session
from drive_presentation import GoogleDrivePresentation
import logging
import webapp2
import httplib2
import jinja2
import json
import os
import models

#############################################
# Configuration
#############################################

config = {
    "client.secrets" : "client_secrets.json",
    "drive.scopes" : (
        'https://www.googleapis.com/auth/drive.readonly '
        'https://www.googleapis.com/auth/userinfo.email '
        'https://www.googleapis.com/auth/userinfo.profile'
    ),
    #"oauth2.callback" : "https://telepromptu.appspot.com/oauth2callback"
    "oauth2.callback" : "http://localhost:8080/oauth2callback"
}

#############################################
# Google API Service Functions
#############################################

def Service(service='drive', version='v2', credentials=None):
    http = httplib2.Http()
    credentials.authorize(http)
    return build(service, version, http=http)

#############################################
# Google Drive Functions
#############################################

class DriveCommunicator:
    def __init__(self, creds):
        self.service = Service(credentials=creds)

    def files(self):
        result = []
        page_token = None
        while True:
            try:
                param = {}
                if page_token:
                    param['pageToken'] = page_token
                files = self.service.files().list(**param).execute()

                result.extend(files['items'])
                page_token = files.get('nextPageToken')
                if not page_token:
                    break
            except errors.HttpError, error:
                print 'An error occurred: %s' % error
                break
        return result

#############################################
# Google OAuth2 Functions
#############################################

def OAuth2Flow():
    flow = flow_from_clientsecrets(config['client.secrets'], scope='')
    flow.redirect_uri = config['oauth2.callback']
    flow.scope = config['drive.scopes']
    return flow

class OAuth2Handler(webapp2.RequestHandler):
    def get(self):
        flow = OAuth2Flow()
        uri = str(flow.step1_get_authorize_url())
        self.redirect(uri)

class OAuth2CallbackHandler(webapp2.RequestHandler):
    def get(self):
        code = self.request.get('code')

        if not code:
            return None

        oauth_flow = OAuth2Flow()

        try:
            creds = oauth_flow.step2_exchange(code)
        except FlowExchangeError:
            return None

        if not creds:
            return None

        users_service = Service(service='oauth2', credentials=creds)
        userid = users_service.userinfo().get().execute().get('id')

        # Store the information
        #user = StorageByKeyName(models.UserCredentials, userid, 'credentials').get()
        #if not user:
        user = StorageByKeyName(models.UserCredentials, userid, 'credentials').put(creds)

        # Store the session
        session = get_current_session()
        session.regenerate_id()
        session['userid'] = userid

        # Redirect
        return self.redirect('/')

#############################################
# Controller Assistant
#############################################

class BaseHandler(webapp2.RequestHandler):
    def __init__(self, request, response):
        super(BaseHandler, self).__init__(request, response)

    def get_drive(self):
        session = get_current_session()
        userid = session.get('userid')

        if not userid:
            logging.info('AUTH_FAIL: NO SESSION')
            return None

        credentials = StorageByKeyName(models.UserCredentials, userid, 'credentials').get()
        
        if not credentials:
            logging.info('AUTH_FAIL: NO CREDENTIALS')
            return None

        drive = DriveCommunicator(credentials)
        return drive

    def remove_auth(self):
        session = get_current_session()
        del session['userid']



#############################################
# List Presentations Page
#############################################

class MainHandler(BaseHandler):
    def get(self):
        try:
            drive = self.get_drive()

            if drive == None:
                return self.redirect('/oauth')

            files = drive.files()

            for f in files:
                if f['mimeType'] == 'application/vnd.google-apps.presentation':
                    text = "<a href=\"%s\">%s</a> <img src=\"%s\" /><br />" % (
                        '/presentation?id='+f['id'],
                        f['title'], 
                        f['thumbnailLink']
                    )
                    self.response.out.write(text)
        except errors.HttpError, error:
            return self.remove_auth()
        except AccessTokenRefreshError, error:
            return self.remove_auth()

#############################################
# View Presentation Page
#############################################

class PresentationHandler(BaseHandler):
    def get(self):
        id = self.request.get('id')

        if not id:
            self.abort('404')
            return

        drive = self.get_drive()
        pres = GoogleDrivePresentation(drive, id)

        slidethumb = self.request.get('slidethumb')
        if slidethumb:
            pres.thumbnail(self, slidethumb)
            return

        html = """
        <script type="text/javascript" src="/static/jquery-1.11.0.min.js"></script>
        <script type="text/javascript" src="%(javascript)s"></script>
        <iframe src="https://docs.google.com/presentation/d/%(id)s/preview"
                id="gdpresentation"
                frameborder="0" 
                width="960" 
                height="569" 
                allowfullscreen="true" 
                mozallowfullscreen="true" 
                webkitallowfullscreen="true">
        </iframe>
        %(debug)s
        """

        self.response.out.write(html % {
                "id" : id,
                "javascript" : "/static/main.js",
                "debug" : str(pres.get_data())
            })

#############################################
# Routes
#############################################

app = webapp2.WSGIApplication([
    webapp2.Route('/', MainHandler),
    webapp2.Route('/presentation', PresentationHandler),
    webapp2.Route('/oauth', OAuth2Handler),
    webapp2.Route('/oauth2callback', OAuth2CallbackHandler)
], debug=True)
