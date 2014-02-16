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
import jinja2
import logging
import webapp2
import httplib2
import jinja2
import json
import os
import models
import time

#############################################
# Configuration
#############################################

config = {
    "client.secrets" : "client_secrets.json",
    "drive.scopes" : (
        'https://www.googleapis.com/auth/drive.readonly '
        'https://www.googleapis.com/auth/userinfo.email '
        'https://www.googleapis.com/auth/userinfo.profile '
        'https://www.googleapis.com/auth/glass.timeline'
    ),
    "oauth2.callback" : "https://telepromptu.appspot.com/oauth2callback"
    #"oauth2.callback" : "http://localhost:8080/oauth2callback"
}

jinja = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

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
                param['q'] = "mimeType = 'application/vnd.google-apps.presentation'"
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
        return self.redirect('/presentations')

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
            template = jinja.get_template('views/index.html')
            self.response.write(template.render({
                    "title" : "Home - Telepromtu"
                }))

        except errors.HttpError, error:
            return self.remove_auth()
        except AccessTokenRefreshError, error:
            return self.remove_auth()

#############################################
# List Presentations Page
#############################################

class PresentationsHandler(BaseHandler):
    def get(self):
        try:
            drive = self.get_drive()

            if drive == None:
                return self.redirect('/oauth')

            files = drive.files()
            pres = []

            for f in files:
                if f['mimeType'] == 'application/vnd.google-apps.presentation':
                    pres.append({
                            "title" : f['title'],
                            "thumbnail" : f['thumbnailLink'],
                            "id" : f['id']
                        })

            template = jinja.get_template('views/presentations.html')
            self.response.write(template.render({
                    "title" : "Presentations - Telepromtu",
                    "files" : pres
                }))

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
            self.abort(404)
            return

        drive = self.get_drive()

        if not drive:
            self.abort(403)
            return

        pres = GoogleDrivePresentation(drive, id)
        finfo = pres.get_info()

        session = get_current_session()
        whatever = models.PresentationLink(driveid=id, userid=session['userid'])
        whatever.put()

        slidethumb = self.request.get('slidethumb')
        if slidethumb:
            pres.thumbnail(self, slidethumb)
            return

        html = """
        <!DOCTYPE HTML><html><head></head><body>
        <script type="text/javascript" src="/static/jquery-1.11.0.min.js"></script>
        <script type="text/javascript" src="/_ah/channel/jsapi"></script>
        <script type="text/javascript" src="%(javascript)s"></script>
        <script>
            channel = new goog.appengine.Channel('%(channel)s');
            socket = channel.open();
            socket.onopen = function(){
            };
            socket.onmessage = function(message){
                /*alert("Data Received: " + message.data);*/
                var data = JSON.parse(message.data);
                if(data.action == "changeSlide") {
                    $("#gdpresentation")[0].src = "https://docs.google.com/presentation/d/%(id)s/preview#slide=id."+data.slideid;
                }
            };
            socket.onerror = function(error){
                alert("Link Failed!");
            };
            socket.onclose = function(){
            };
            socket.sendMessage = function(path, opt_param) {
              path += '?g=' + state.game_key;
              if (opt_param) {
                path += '&' + opt_param;
              }
              var xhr = new XMLHttpRequest();
              xhr.open('POST', path, true);
              xhr.send();
            };

        </script>
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
        </body></html>
        """

        channel_token = channel.create_channel(id)

        template = jinja.get_template('views/slideshow.html')
        self.response.write(template.render({
                "title" : "%s - Telepromtu" % finfo['title'],
                "id" : id,
                "channel" : channel_token,
                "file" : finfo
            }))

        """self.response.out.write(html % {
                "id" : id,
                "javascript" : "/static/main.js",
                "debug" : str(pres.get_data()),
                "channel" : channel_token
            })"""

        

#############################################
# Channel: Browser to Server
#############################################

def UpdateSlide(driveid, slideid):
    channel.send_message(
        driveid,
        json.dumps({
                "action" : "changeSlide",
                "slideid" : slideid
            })
    )

#############################################
# Channel: Browser to Server
#############################################

#############################################
# Channel: Glass to Server
#############################################

class GlassDataHandler(BaseHandler):
    def get(self):
        try:
            driveid = self.request.get('id')

            if not driveid:
                return self.abort(401)

            preslink = models.PresentationLink.gql("WHERE driveid = :1", driveid).get()
            userid = preslink.userid

            if not userid:
                return self.abort(404)

            credentials = StorageByKeyName(models.UserCredentials, userid, 'credentials').get()
            
            if not credentials:
                return self.abort(403)

            drive = DriveCommunicator(credentials)
            
            presentation = GoogleDrivePresentation(drive, driveid)

            self.response.headers['Content-Type'] = 'application/json'
            self.response.out.write(json.dumps(presentation.get_data()))

        except errors.HttpError, error:
            return self.remove_auth()
        except AccessTokenRefreshError, error:
            return self.remove_auth()

class GlassDataHandlerSlides(BaseHandler):
    def get(self):
        driveid = self.request.get('id')
        slideid = self.request.get('slide')

        if not driveid or not slideid:
            return self.abort(401)

        UpdateSlide(driveid, slideid)

#############################################
# Push: Server to Glass
#############################################

class PresenterHandler(BaseHandler):
    def get(self):
        driveid = self.request.get('id')

        if not driveid:
            return self.abort(401)

        NotifyGlass(driveid)

        self.response.out.write('OK')

# Let glass know when a new presentation is available
def NotifyGlass(driveid):
    session = get_current_session()
    userid = session.get('userid')

    if not userid:
        logging.info('AUTH_FAIL: NO SESSION')
        return None

    credentials = StorageByKeyName(models.UserCredentials, userid, 'credentials').get()
    
    if not credentials:
        logging.info('AUTH_FAIL: NO CREDENTIALS')
        return None

    mirror = Service('mirror', 'v1', credentials)

    body = {
      "id" : "telepromptu-unique-"+str(time.time()),
      "title" : "Telepromptu",
      "text": "Telepromptu: You have a new presentation available. Choose the open option to continue.",
      "notification": {
        "level": "DEFAULT"
      },
      "menuItems" : [
        {
            "id" : "telepromptu-notification-unique-"+str(time.time()),
            "action" : "OPEN_URI",
            "payload" : "http://telepromptu.com/presentation?id=%s" % (driveid),
            "values" : [
                {
                    "state" : "default",
                    "displayName": "Start Presentation"
                }
            ]
        }
      ]
    }

    mirror.timeline().insert(body=body).execute()

    logging.info("dispatched message to %s", userid)

#############################################
# Routes
#############################################

app = webapp2.WSGIApplication([
    webapp2.Route('/', MainHandler),
    webapp2.Route('/presentation', PresentationHandler),
    webapp2.Route('/presentations', PresentationsHandler),
    webapp2.Route('/glass', GlassDataHandler),
    webapp2.Route('/glass-slides', GlassDataHandlerSlides),
    webapp2.Route('/present', PresenterHandler),
    webapp2.Route('/oauth', OAuth2Handler),
    webapp2.Route('/oauth2callback', OAuth2CallbackHandler)
], debug=True)
