#!/bin/sh

# MongoDB Java driver won't run authentication twice on the same DB instance,
# so we need to use multiple DBs.
mongo --eval 'db.createUser({"user": "clojurewerkz/monger", "pwd": "monger", roles: ["dbAdmin"], mechanisms: ["SCRAM-SHA-1"], passwordDigestor: "client"})' users
mongo --eval 'db.createUser({"user": "clojurewerkz/monger", "pwd": "monger", roles: ["dbAdmin"], mechanisms: ["SCRAM-SHA-1"], passwordDigestor: "client"})' users2
mongo --eval 'db.createUser({"user": "clojurewerkz/monger", "pwd": "monger", roles: ["dbAdmin"], mechanisms: ["SCRAM-SHA-1"], passwordDigestor: "client"})' users3
mongo --eval 'db.createUser({"user": "clojurewerkz/monger", "pwd": "monger", roles: ["dbAdmin"], mechanisms: ["SCRAM-SHA-1"], passwordDigestor: "client"})' users4
