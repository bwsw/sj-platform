//import {Injectable} from 'angular2/core';
//import {Observable} from 'rxjs/Rx';
//
//@Injectable()
//export class UploadService {
//  constructor () {
//    this.progress$ = Observable.create(observer => {
//      this.progressObserver = observer
//    }).share();
//  }
//
//  private makeFileRequest (url: string, file: File): Observable {
//
//    return Observable.create(observer => {
//      let formData: FormData = new FormData(),
//        xhr: XMLHttpRequest = new XMLHttpRequest();
//        formData.append("jar", file);
//
//
//      xhr.onreadystatechange = () => {
//        //if (xhr.readyState === 4) {
//        //  if (xhr.status === 200) {
//        //    observer.next(JSON.parse(xhr.response));
//        //    observer.complete();
//        //  } else {
//        //    observer.error(xhr.response);
//        //  }
//        //}
//      };
//
//      xhr.upload.onprogress = (event) => {
//        this.progress = Math.round(event.loaded / event.total * 100);
//        this.progressObserver.next(this.progress);
//      };
//
//      xhr.open('POST', url, true);
//      xhr.send(formData);
//    });
//  }
//}
