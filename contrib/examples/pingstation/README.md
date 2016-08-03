# Ping Station Demo

Assuming that all components of the SJ infrastructure is deployed.

## Preparation

At the beginning you should create streams that will be used in the instances of input, process and output modules by sending several post requests to 'http://<rest-api-address>/v1/streams' with the following content:

1. [The content](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/streams/echo-response.json)
to create an output stream of input module (consequently, an input stream of process module) that will be used for keeping an IP and average time from ICMP echo response and also timestamp of the event
2. [The content](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/streams/unreachable-response.json)
to create another output stream of input module (consequently, an input stream of process module) that will be used for keeping an IP from ICMP unreachable response and also timestamp of the event
3. [The content](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/streams/echo-response-1m.json)
to create an output stream of process module (consequently, an input stream of output module) that will be used for keeping an aggregated information for each IP (by minute)
about average time of echo response, total amount of echo responses, total amount of unreachable responses and the timestamp
4. [The content](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/streams/es-echo-response-1m.json)
to create an output stream of output module that will be used for keeping an aggregated information (by minute) from previous stream including total amount of responses

Then you should create an instance for each module:

1. For creating an instance of input module send a post request to 'http://<rest-api-address>/v1/modules/input-streaming/pingstation-input/0.1/instance' with the following
[content](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/instances/pingstation-input.json)
2. For creating an instance of process module send a post request to 'http://<rest-api-address>/v1/modules/regular-streaming/pingstation-process/0.1/instance' with the following
[content](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/instances/pingstation-process.json)
3. For creating an instance of output module send a post request to 'http://<rest-api-address>/v1/modules/output-streaming/pingstation-output/0.1/instance' with the following
[content](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/instances/pingstation-output.json)

## Launching

After that you can launch the every module:

1. For launching the input module send a get request to 'http://<rest-api-address>/v1/modules/input-streaming/pingstation-input/0.1/instance/pingstation-input/start'
2. For launching the process module send a get request to 'http://<rest-api-address>/v1/modules/regular-streaming/pingstation-process/0.1/instance/pingstation-process/start'
3. For launching the output module send a get request to 'http://<rest-api-address>/v1/modules/output-streaming/pingstation-output/0.1/instance/pingstation-output/start'

To get a list of listening ports of input module, send a get request to 'http://<rest-api-address>/v1/modules/input-streaming/pingstation-input/0.1/instance/pingstation-input'
and look at field named tasks, e.g. it will look like
> "tasks": {   
> &nbsp;&nbsp;"pingstation-input-task0": {   
> &nbsp;&nbsp;&nbsp;&nbsp;"host": "176.120.25.19",  
> &nbsp;&nbsp;&nbsp;&nbsp;"port": 31000   
> &nbsp;&nbsp;},   
> &nbsp;&nbsp;"pingstation-input-task1": {   
> &nbsp;&nbsp;&nbsp;&nbsp;"host": "176.120.25.19",   
> &nbsp;&nbsp;&nbsp;&nbsp;"port": 31004   
> &nbsp;&nbsp;}   
> &nbsp;}   

And now you can start a flow by invoking `fping -l -g 91.221.60.0/23 2>&1 | nc 176.120.25.19 31000` from your terminal.

## Shutdown

To stop the modules:

1. For stopping the input module send a get request to 'http://<rest-api-address>/v1/modules/input-streaming/pingstation-input/0.1/instance/pingstation-input/stop'
2. For stopping the process module send a get request to 'http://<rest-api-address>/v1/modules/regular-streaming/pingstation-process/0.1/instance/pingstation-process/stop'
3. For stopping the output module send a get request to 'http://<rest-api-address>/v1/modules/output-streaming/pingstation-output/0.1/instance/pingstation-output/stop'

## Customization

If you want to change an aggregation interval:

1. Create two additional streams like 'echo-response-1m' and 'es-echo-response-1m'
(e.g. ['echo-response-3m'](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/streams/echo-response-3m.json) and
['es-echo-response-3m'](https://github.com/bwsw/sj-platform/contrib/examples/pingstation/api-json/streams/es-echo-response-3m.json))
2. Create an instance of process module only changing the 'checkpoint-interval' at the corresponding time (in milliseconds)

After that you can launch this instance as described above in the second point of launching section
(don't forget to change the instance name: 'pingstation-process' -> <new instance name> in the request line).

## Additionally

If you decide to deploy SJ infrastructure on your own server, you should fill the database from the ground up.
