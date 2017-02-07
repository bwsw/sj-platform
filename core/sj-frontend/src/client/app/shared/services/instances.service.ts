import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import {
  InstanceModel,
  SubtypedInstance,
  RegularStreamingInstance,
  OutputStreamingInstance,
  InputStreamingInstance,
  WindowedStreamingInstance
} from '../models/instance.model';
import { TaskModel } from '../models/task.model';
import { BaseResponse } from '../models/base-response.model';

interface ITasksObject {
  tasks: TaskModel[];
  message: string;
}

@Injectable()
export class InstancesService {
  private dataUrl = '/v1/';

  private static fillInstanceGeneralFields(orig: InstanceModel, instance: SubtypedInstance) {
    instance.name = orig.name;
    instance.description = orig.description;
    // Checking if string 'max' or numeric is passed
    instance.parallelism = /^\+?(0|[1-9]\d*)$/.test(orig.parallelism)
      ? parseInt(orig.parallelism)
      : orig.parallelism;
    instance.options = orig.options;
    instance.perTaskCores = orig.perTaskCores;
    instance.perTaskRam = orig.perTaskRam;
    instance.jvmOptions = orig.jvmOptions;
    instance.nodeAttributes = orig.nodeAttributes;
    instance.coordinationService = orig.coordinationService;
    instance.environmentVariables = orig.environmentVariables;
    instance.performanceReportingInterval = orig.performanceReportingInterval;

    return instance;
  }

  constructor(private http: Http) { }

  public getInstanceList(): Observable<InstanceModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'modules/instances', options)
      .map(response => {
        const data = this.extractData(response);
        return data.instances;
      })
      .catch(this.handleError);
  }

  public getInstanceInfo(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'modules/' + instance.moduleType + '/' + instance.moduleName + '/' +
      instance.moduleVersion + '/instance' + '/' + instance.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data.instance;
      })
      .catch(this.handleError);
  }

  public getInstanceTasks(instance: InstanceModel): Observable<ITasksObject> {
    console.log(instance.restAddress);
    return this.http.get('http://'+instance['rest-address'])
      .map(this.extractData)
      .catch(this.handleError);
  }

  public saveInstance(instance: InstanceModel) {
    let subtypedInstance = this.getPreparedInstance(instance);
    let instance_body = Object.assign({}, subtypedInstance);
    let body = JSON.stringify(instance_body, this.cleanupBodyValues);
    let headers = new Headers({'Content-Type': 'application/json'});
    let options = new RequestOptions({ headers: headers });

    return this.http.post(this.dataUrl + 'modules/' + instance.module.moduleType + '/' + instance.module.moduleName + '/' +
      instance.module.moduleVersion + '/instance', body, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  public deleteInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this.dataUrl + 'modules/' + instance.module.moduleType + '/' + instance.module.moduleName + '/' +
    instance.module.moduleVersion + '/instance' + '/' + instance.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  public startInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'modules/' + instance.module.moduleType + '/' + instance.module.moduleName + '/' +
      instance.module.moduleVersion + '/instance' + '/' + instance.name + '/start', options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  public stopInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'modules/' + instance.module.moduleType + '/' + instance.module.moduleName + '/' +
      instance.module.moduleVersion + '/instance' + '/' + instance.name + '/stop', options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  private getPreparedInstance(orig: InstanceModel) {
    let inst: SubtypedInstance;

    switch (orig.module.moduleType) {

      case 'regular-streaming':
        inst = new RegularStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        inst.checkpointMode = orig.checkpointMode;
        inst.checkpointInterval = orig.checkpointInterval;
        inst.eventWaitIdleTime = orig.eventWaitIdleTime;
        orig.inputs.forEach(function(item:string, i:number) {
          inst.inputs[i] = orig.inputs[i] + '/' + orig.inputsTypes[i];
        });
        inst.outputs = orig.outputs;
        inst.stateManagement = orig.stateManagement;
        inst.startFrom = orig.startFrom === 'timestamp' ? orig.startFromTimestamp : orig.startFrom;
        inst.stateFullCheckpoint = orig.stateFullCheckpoint

        break;

      case 'windowed-streaming':
        inst = new WindowedStreamingInstance();

        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        inst.outputs = orig.outputs;
        delete inst.inputs;
        inst.stateManagement = orig.stateManagement;
        inst.window = orig.window;
        inst.stateFullCheckpoint = orig.stateFullCheckpoint;
        inst.eventWaitIdleTime = orig.eventWaitIdleTime;
        inst.mainStream = orig.mainStream + '/' + orig.mainStreamType;
        if (orig.relatedStreams.length > 0 && orig.relatedStreams[0] !== '') {
          orig.relatedStreams.forEach(function(item:string, i:number) {
            inst.relatedStreams[i] = orig.relatedStreams[i] + '/' + orig.relatedStreamsType[i];
          });
        } else {
          inst.relatedStreams = [];
        }
        inst.batchFillType = { typeName: orig.batchFillType.typeName, value: orig.batchFillType.value}
        inst.slidingInterval = orig.slidingInterval;
        inst.startFrom = orig.startFrom === 'timestamp' ? orig.startFromTimestamp : orig.startFrom;
        break;

      case 'output-streaming':
        inst = new OutputStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        delete inst.inputs;
        delete inst.outputs;
        inst.checkpointMode = orig.checkpointMode;
        inst.checkpointInterval = orig.checkpointInterval;
        inst.input = orig.input;
        inst.output = orig.output;
        inst.startFrom = orig.startFrom === 'timestamp' ? orig.startFromTimestamp : orig.startFrom;
        break;

      case 'input-streaming':
        inst = new InputStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        delete inst.inputs;
        inst.asyncBackupCount = orig.asyncBackupCount;
        inst.backupCount = orig.backupCount;
        inst.checkpointInterval = orig.checkpointInterval;
        inst.checkpointMode = orig.checkpointMode;
        inst.defaultEvictionPolicy = orig.defaultEvictionPolicy;
        inst.duplicateCheck = orig.duplicateCheck;
        inst.evictionPolicy = orig.evictionPolicy;
        inst.lookupHistory = orig.lookupHistory;
        inst.outputs = orig.outputs;
        inst.queueMaxSize = orig.queueMaxSize;
        break;
    }

    let objectFields = ['environmentVariables', 'jvmOptions', 'nodeAttributes', 'options'];
    for (let fieldName of objectFields) {
      if (inst[fieldName] !== undefined) {
        inst[fieldName] = inst[fieldName].length === 0 ? '{}' : inst[fieldName];
        inst[fieldName] = JSON.parse(inst[fieldName].toString());
      }
    }
    return inst;
  }

  private extractData(res: Response) {
    let body = new BaseResponse();
    if (res.json()['entity'] !== 'undefined') {
      body.fillFromJSON(res.json()['entity']);
      return body;
    }
    return body = res.json();
  }

  private handleError(error: any) {
    let errMsg = (error._body) ? error._body :
      error.status ? `${error.status} - ${error.statusText}` : 'Server error';
    if (typeof errMsg !== 'object') { errMsg = JSON.parse(errMsg); }
    let errMsgYo = errMsg.entity ? errMsg.entity.message : "Undefined error";
    return Observable.throw(errMsgYo);
  }


  private cleanupBodyValues(key: string, value: any): any {
    if ( [null, ''].indexOf(value) > -1 ) {
      return undefined;
    } else {
      return value;
    }
  }


}
