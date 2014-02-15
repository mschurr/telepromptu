
from HTMLParser import HTMLParser
import logging
import urllib2
import re

PRESENTATION_URI = "https://docs.google.com/presentation/d/%(id)s/preview"
SLIDE_URI = "https://docs.google.com/presentation/d/%(id)s/preview#slide=id.%(slideid)s"
HTML_URI = "https://docs.google.com/presentation/d/%(id)s/htmlpresent"
PNG_API = "https://docs.google.com/presentation/d/%(id)s/export/png?id=%(id)s&pageid=%(slideid)s"

class GDParser(HTMLParser):
    pageidregex = re.compile(r'pageid=(.+?)&')
    regex1 = re.compile(r'background\-image: url\((.+)\);')

    def __init__ (self, driveid):
        self.driveid = driveid
        self.pageids = []
        self.imgurls = []
        self.notes = []
        self.noteson = False
        self.reset()

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'section':
            if attrs.get('class', '') == 'slide-content':
                stylestr = attrs['style']
                url = HTMLParser().unescape(GDParser.regex1.search(stylestr).group(1))
                pageid = GDParser.pageidregex.search(url).group(1)
                imgurl = PNG_API % {
                    "id" : self.driveid,
                    "slideid" : pageid
                }
                self.pageids.append(pageid)
                self.imgurls.append(imgurl)
                self.notes.append('')
                self.noteson = False
            if attrs.get('class', '') == 'slide-notes':
                self.curaccum = ''
                self.noteson = True

    def handle_endtag(self, tag):
        pass

    def handle_data(self, data):
        if self.noteson:
            self.notes[-1] += HTMLParser().unescape(data) + ' '

    def get_slides(self):
        ret = []
        for i in range(len(self.pageids)):
            ret.append({'img_url': self.imgurls[i], 'page_id': self.pageids[i], 'speaker_notes': self.notes[i]})
        return ret

class GoogleDrivePresentation:
    def __init__(self, drive, id):
        self.id = id
        self.drive = drive
        self.service = drive.service

    def get_data(self):
        resp, content = self.service._http.request(self.get_html_url())
        if resp.status != 200:
            return None

        # Parse HTML Code
        html = content.splitlines()[-1]
        parser = GDParser(self.id)
        parser.feed(html)
        slides = parser.get_slides()
        return slides

    def get_html_url(self):
        return HTML_URI % {
            "id" : self.id
        }

    def thumbnail(self, handler, slideid):
        url = PNG_API % {
            "id" : self.id,
            "slideid" : slideid
        }

        resp, content = self.service._http.request(url)
        if resp.status != 200:
            handler.abort(500)

        handler.response.headers['Content-Type'] = 'image/png'
        handler.response.out.write(content)

    def get_presentation_url(self):
        return PRESENTATION_URI % {
            "id" : self.id
        }

    def get_slide_url(self, slideid):
        return SLIDE_URI % {
            "id" : self.id,
            "slideid" : slideid
        }