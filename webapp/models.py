from google.appengine.ext import db
from oauth2client.appengine import CredentialsProperty

class UserCredentials(db.Model):
	credentials = CredentialsProperty()