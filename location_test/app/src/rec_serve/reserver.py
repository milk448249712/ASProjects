#!/usr/bin/python
# -*- coding: UTF-8 -*-

import tornado.httpserver
import tornado.ioloop
import tornado.options
import tornado.web
import os
import sqlite3

from tornado.options import define, options
 
print("你好，世界")
define("port", default=9998, help="run on the given port", type=int)

class IndexHandler(tornado.web.RequestHandler):
    global MOBILE_STARTUP_NODES
    def get(self):   
        lat = self.get_argument('lat', 'no lat')
        long = self.get_argument('long', 'no long')
        target = self.get_argument('target', 'no target')
        self.write('<p>lat:['+lat+']</p>')
        self.write('<p>long:['+long+']</p>')
        self.write('<p>target:['+target+']</p>')
        conn = sqlite3.connect('locationManage.db')
        c = conn.cursor()
        cursor = c.execute('''
            SELECT count(*) FROM sqlite_master
            WHERE type='table' AND
            name='location';''')
        print("cursor:",cursor)
        for row in cursor:
            print("count:",row[0])
            if row[0] == 1 :
                print("table is exists.")
                break
            else :
                c.execute('''CREATE TABLE location
                    (ID INTEGER PRIMARY KEY AUTOINCREMENT     NOT NULL,
                    lat           REAL    NOT NULL,
                    long            REAL     NOT NULL,
                    target        TEXT,
                    time         INTEGER);''')
                conn.commit()
                print("Table created successfully")
                break
        c.execute("INSERT INTO location (ID,lat,long,target,time) \
            VALUES (NULL, 123.34, 567.89, '"+lat+"', "+long+" )");
        conn.commit()
        cursor = c.execute("SELECT ID, lat, long, target, time  from location")
        for row in cursor:
           self.write("<p>ID = "+str(row[0]))
           self.write("lat = "+str(row[1]))
           self.write("long = "+str(row[2]))
           self.write("target = "+row[3])
           self.write("time = "+str(row[4])+"</p>")
        conn.close()

def main():
    tornado.options.parse_command_line()
    application = tornado.web.Application([(r"/location",IndexHandler)])
    http_server = tornado.httpserver.HTTPServer(application)
    http_server.listen(options.port)
    tornado.ioloop.IOLoop.current().start()

if __name__ == "__main__":
    main()