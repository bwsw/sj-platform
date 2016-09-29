export class ProviderModel {
  name: string;
  description: string;
  login: string;
  password: string;
  type: string;
  hosts: [string] = [''];
}
