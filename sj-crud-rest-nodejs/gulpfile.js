var gulp = require('gulp');
var config = require('./config.json');
var pm2 = require('pm2');

gulp.task('api::start', function() {
    console.log('Starting api...');
    pm2.connect(function(err) {
        if (err) {
            console.error(err);
            process.exit(1);
        }

        pm2.start({
            script: config.api.pm2.script,
            name: config.api.pm2.name,
            max_memory_restart: config.api.pm2.max_memory_restart,
            watch: true
        }, function(err) {
            if (err) {
                console.error(err);
                process.exit(1);
            }

            pm2.streamLogs('all', 0);
        });
    });
});

gulp.task('api::stop', function() {
        console.log('Stopping api...');

    pm2.connect(function(err) {
        if (err) {
            console.error(err);
            process.exit(1);
        }

        // @todo Should we stop and delete app in context of pm2?
        // @todo if there is no app name we should return 'Already stopped'
        pm2.stop(config.api.pm2.name, function(err) {
            if (err) {
                console.error(err);
                process.exit(1);
            }

            pm2.delete(config.api.pm2.name, function(err) {
                if (err) {
                    console.error(err);
                    process.exit(1);
                }

                // @todo Should we disconnect pm2?
                pm2.disconnect(function() { process.exit(0) });
            });
        });
    });
});

gulp.task('api::restart', function() {
    gulp.start('api::stop', ['api::start']);
});