import { ProviderModel } from './provider.model';
import { ServiceModel } from './service.model';
import { StreamModel } from "./stream.model";
import { InstanceModel } from "./instance.model";
import { ModuleModel } from "./module.model";
import { SettingModel } from "./setting.model";
import { CustomFileModel, CustomJarModel } from "./custom.model";

export class BaseResponse<>  {
  [key: string]: any;
  message: string;
  connection: boolean;
  types: string[];
  providers: ProviderModel[];
  services: ServiceModel[];
  streams: StreamModel[];
  instances: InstanceModel[];
  modules: ModuleModel[];
  configSettings: SettingModel[];
  customFiles: CustomFileModel[];
  customJars: CustomJarModel[];
  specification: ModuleModel;
  instance: InstanceModel;
  domains: string[];

  fillFromJSON(json: any) {
    for (let propName in json) {
      this[propName] = json[propName]
    }
  }
}
