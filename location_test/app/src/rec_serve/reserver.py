#!/usr/bin/python
# -*- coding: UTF-8 -*-

# python tornado web服务，用于接受transmit.php发来的位置信息，并将信息存入数据库（后期需要做成后台程序）
import tornado.httpserver
import tornado.ioloop
import tornado.options
import tornado.web
import os
import sqlite3
import time, datetime

from tornado.options import define, options
 
print("你好，世界")
define("port", default=9998, help="run on the given port", type=int)

class IndexHandler(tornado.web.RequestHandler):
    global MOBILE_STARTUP_NODES
    def get(self):   
        lat = self.get_argument('lat', 'no lat')
        long = self.get_argument('long', 'no long')
        target = self.get_argument('target', 'no target')
        #self.write('<p>lat:['+lat+']</p>')
        #self.write('<p>long:['+long+']</p>')
        #self.write('<p>target:['+target+']</p>')
        conn = sqlite3.connect('locationManage.db')
        c = conn.cursor()
        cursor = c.execute('''
            SELECT count(*) FROM sqlite_master
            WHERE type='table' AND
            name='location';''')
        for row in cursor:
            if row[0] == 1 :
                print("table is exists.")
                break
            else :
                c.execute('''CREATE TABLE location
                    (ID INTEGER PRIMARY KEY AUTOINCREMENT     NOT NULL,
                    lat           REAL    NOT NULL,
                    long            REAL     NOT NULL,
                    target        TEXT,
                    recv_time         TEXT);''')
                conn.commit()
                print("Table created successfully")
                break
        sql_cmd = "INSERT INTO location (ID,lat,long,target,recv_time)"
        sql_cmd += " VALUES (NULL, {}, {}".format(lat,long)
        sql_cmd += ",\"{}\",{});".format(target,"strftime('%s','now')")
        print(sql_cmd)
        try:
            c.execute(sql_cmd)
            conn.commit()
            cursor = c.execute("SELECT ID, lat, long, target, recv_time  from location")
            #for row in cursor:
            #   self.write("<p>ID = "+str(row[0]))
            #   self.write("lat = "+str(row[1]))
            #   self.write("long = "+str(row[2]))
            #   self.write("target = "+row[3])
            #   self.write("recv_time = "+str(row[4])+"</p>")
            conn.close()
            self.write('LHS SUCESS.')
            print("LHS SUCESS.\n")
        except Exception as e:
            self.write('LHS ERROR:'+str(e))
            print("LHS ERROR.", e)

def main():
    tornado.options.parse_command_line()
    application = tornado.web.Application([(r"/location",IndexHandler)])
    http_server = tornado.httpserver.HTTPServer(application)
    http_server.listen(options.port)
    tornado.ioloop.IOLoop.current().start()

if __name__ == "__main__":
    main()