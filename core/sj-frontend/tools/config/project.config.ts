import { join } from 'path';
import { SeedConfig } from './seed.config';

const proxy = require('proxy-middleware');

/**
 * This class extends the basic seed configuration, allowing for project specific overrides. A few examples can be found
 * below.
 */
export class ProjectConfig extends SeedConfig {

  PROJECT_TASKS_DIR = join(process.cwd(), this.TOOLS_DIR, 'tasks', 'project');

  constructor() {
    super();

    this.APP_TITLE = 'Stream Juggler';

    /* Enable typeless compiler runs (faster) between typed compiler runs. */
    // this.TYPED_COMPILE_INTERVAL = 5;

    this.SYSTEM_BUILDER_CONFIG.packages['ng2-bootstrap'] = {
      main: 'ng2-bootstrap.js',
      defaultExtension: 'js'
    };

    this.SYSTEM_BUILDER_CONFIG.packages['moment'] = {
      main: 'moment.js',
      defaultExtension: 'js'
    };

    // Add `NPM` third-party libraries to be injected/bundled.
    this.NPM_DEPENDENCIES = [
      ...this.NPM_DEPENDENCIES,
      { src: 'ng2-bootstrap/bundles/ng2-bootstrap.min.js', inject: 'libs' },
      { src: 'moment/min/moment.min.js', inject: 'libs' }
      // {src: 'jquery/dist/jquery.min.js', inject: 'libs'},
      // {src: 'lodash/lodash.min.js', inject: 'libs'},
    ];

    // Add `local` third-party libraries to be injected/bundled.
    this.APP_ASSETS = [
      ...this.APP_ASSETS,
      // {src: `${this.APP_SRC}/your-path-to-lib/libs/jquery-ui.js`, inject: true, vendor: false}
      // {src: `${this.CSS_SRC}/path-to-lib/test-lib.css`, inject: true, vendor: false},
    ];

    /* Add to or override NPM module configurations: */
    /* @todo: Fetch API url from environment config */
    this.mergeObject(this.PLUGIN_CONFIGS['browser-sync'], {
      middleware: [
        proxy({
          hostname: '176.120.25.19',
          port: 28080,
          pathname: '/v1',
          route: '/v1'
        }),
        require('connect-history-api-fallback')({ index: `${this.APP_BASE}index.html` })
      ]
    });
  }

}
