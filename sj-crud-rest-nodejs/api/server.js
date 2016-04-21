var config = require('./../config.json');

var bodyParser = require('body-parser');
var express = require('express');
var app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({'extended': true}));

var VERSIONS = { alpha: '/v0' };

app.route('/')
    .get(function (req, res) {
        res.json(VERSIONS);
    });

Object.keys(VERSIONS).forEach(function (k) {
    app.use(VERSIONS[k], require('./versions' + VERSIONS[k] + VERSIONS[k]));
});
app.listen(config.api.port);
console.log('Listening :' + config.api.port);
console.log('Server started');
