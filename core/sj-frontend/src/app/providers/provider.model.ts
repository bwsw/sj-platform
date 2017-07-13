import { BaseModel } from '../shared';

export class ProviderModel extends BaseModel {
  name: string;
  description: string;
  login: string;
  password: string;
  type: string;
  hosts: string[] = [''];
  driver: string;
}
