import { Component, AfterViewInit } from '@angular/core';
declare const jquery: any;
declare const $: any;

@Component({
  selector: 'sj-toolbar',
  templateUrl: 'toolbar.component.html'
})
export class ToolbarComponent implements AfterViewInit {

  public ngAfterViewInit() {
    const self = this;
    $('.navbar-toggler').click(function() {
      const bodyClass = localStorage.getItem('body-class');

      if ($(this).hasClass('layout-toggler') && $('body').hasClass('sidebar-off-canvas')) {
        $('body').toggleClass('sidebar-opened').parent().toggleClass('sidebar-opened');
        // resize charts
        self.resizeBroadcast();

      } else if ($(this).hasClass('layout-toggler') && ($('body').hasClass('sidebar-nav') || bodyClass === 'sidebar-nav')) {
        $('body').toggleClass('sidebar-nav');
        localStorage.setItem('body-class', 'sidebar-nav');
        if (bodyClass === 'sidebar-nav') {
          localStorage.clear();
        }
        // resize charts
        self.resizeBroadcast();
      } else {
        $('body').toggleClass('mobile-open');
      }
    });

    $('.aside-toggle').click(() => {
      $('body').toggleClass('aside-menu-open');

      // resize charts
      this.resizeBroadcast();
    });

    $('.sidebar-close').click(() => {
      $('body').toggleClass('sidebar-opened').parent().toggleClass('sidebar-opened');
    });

    /* ---------- Disable moving to top ---------- */
    $('a[href="#"][data-top!=true]').click((e: any) => {
      e.preventDefault();
    });
  }

  private resizeBroadcast() {
    let timesRun = 0;
    const interval = setInterval((e) => {
      timesRun += 1;
      if (timesRun === 5) {
        clearInterval(interval);
      }
      window.dispatchEvent(new Event('resize'));
    }, 62.5);
  }
}
