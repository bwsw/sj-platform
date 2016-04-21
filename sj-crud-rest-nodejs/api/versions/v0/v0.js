// Version 0

var express = require('express');
var app = module.exports = express();

var ENTITIES = {
    providers: '/providers',
    services: '/services',
    streams: '/streams'
};

app.route('/')
    .get(function (req, res) {
        res.json(ENTITIES);
    });

Object.keys(ENTITIES).forEach(function (k) {
    require('./' + k)(app);
});