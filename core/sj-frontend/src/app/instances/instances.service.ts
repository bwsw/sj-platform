import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { BService, BaseService, IResponse } from '../shared/service/base.service';
import {
  InstanceModel,
  SubtypedInstance,
  RegularStreamingInstance,
  BatchStreamingInstance,
  OutputStreamingInstance,
  InputStreamingInstance
} from './instance.model';
import { TaskModel } from './task.model';
import { BaseModel } from '../shared';


interface ITasksObject {
  tasks: TaskModel[];
  message: string;
}

@Injectable()
@BService({
  entity: 'modules',
  entityModel: InstanceModel
})
export class InstancesService extends BaseService<InstanceModel> {

  private static fillInstanceGeneralFields(orig: InstanceModel, instance: SubtypedInstance) {
    instance.name = orig.name;
    instance.description = orig.description;
    // Checking if string 'max' or numeric is passed
    instance.parallelism = /^\+?(0|[1-9]\d*)$/.test(orig.parallelism)
      ? parseInt(orig.parallelism, 0)
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

  public getInstanceInfo(instance: InstanceModel): Observable<InstanceModel> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
    return this.http.get(this.requestUrl + '/' + instance.moduleType + '/' + instance.moduleName + '/' +
      instance.moduleVersion + '/instance' + '/' + instance.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data.instance;
      })
      .catch(this.handleError);
  }

  public getInstanceTasks(instance: InstanceModel): Observable<ITasksObject> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
    return this.http.get(this.requestUrl + '/' + instance.moduleType + '/' + instance.moduleName + '/' +
      instance.moduleVersion + '/instance' + '/' + instance.name + '/tasks', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public saveInstance(instance: InstanceModel): Observable<IResponse<BaseModel>> {
    const subtypedInstance = this.getPreparedInstance(instance);
    const instance_body = Object.assign({}, subtypedInstance);
    const body = JSON.stringify(instance_body, this.cleanupBodyValues);
    const headers = new Headers({'Content-Type': 'application/json'});
    const options = new RequestOptions({ headers: headers });

    return this.http.post(this.requestUrl + '/' + instance.module.moduleType + '/' + instance.module.moduleName + '/' +
      instance.module.moduleVersion + '/instance', body, options)
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }

  public deleteInstance(instance: InstanceModel): Observable<IResponse<BaseModel>> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
    return this.http.delete(this.requestUrl + '/' + instance.moduleType + '/' + instance.moduleName + '/' +
    instance.moduleVersion + '/instance' + '/' + instance.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }

  public startInstance(instance: InstanceModel): Observable<IResponse<BaseModel>> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
    return this.http.get(this.requestUrl + '/' + instance.moduleType + '/' + instance.moduleName + '/' +
      instance.moduleVersion + '/instance' + '/' + instance.name + '/start', options)
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }

  public stopInstance(instance: InstanceModel): Observable<IResponse<BaseModel>> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
    return this.http.get(this.requestUrl + '/' + instance.moduleType + '/' + instance.moduleName + '/' +
      instance.moduleVersion + '/instance' + '/' + instance.name + '/stop', options)
      .map(response => {
        const data = this.extractData(response);
        return data;
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
        orig.inputs.forEach(function (item: string, i: number) {
          inst.inputs[i] = orig.inputs[i] + '/' + orig.inputsTypes[i];
        });
        inst.outputs = orig.outputs;
        inst.stateManagement = orig.stateManagement;
        inst.startFrom = orig.startFrom === 'datetime' ? orig.startFromDateTime : orig.startFrom;
        inst.stateFullCheckpoint = orig.stateFullCheckpoint;

        break;

      case 'batch-streaming':
        inst = new BatchStreamingInstance();

        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        inst.outputs = orig.outputs;
        inst.inputs = orig.inputs;
        inst.stateManagement = orig.stateManagement;
        inst.window = orig.window;
        inst.stateFullCheckpoint = orig.stateFullCheckpoint;
        inst.eventWaitIdleTime = orig.eventWaitIdleTime;
        orig.inputs.forEach(function (item: string, i: number) {
          inst.inputs[i] = orig.inputs[i] + '/' + orig.inputsTypes[i];
        });
        inst.slidingInterval = orig.slidingInterval;
        inst.startFrom = orig.startFrom === 'datetime' ? orig.startFromDateTime : orig.startFrom;
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
        inst.startFrom = orig.startFrom;
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

    const objectFields = ['environmentVariables', 'jvmOptions', 'nodeAttributes', 'options'];
    for (const fieldName of objectFields) {
      if (inst[fieldName] !== undefined) {
        inst[fieldName] = inst[fieldName] === null || inst[fieldName].length === 0 ? '{}' : inst[fieldName];
        inst[fieldName] = JSON.parse(inst[fieldName].toString());
      }
    }
    return inst;
  }

  private cleanupBodyValues(key: string, value: any): any {
    if ( [null, ''].indexOf(value) > -1 ) {
      return undefined;
    } else {
      return value;
    }
  }

}
