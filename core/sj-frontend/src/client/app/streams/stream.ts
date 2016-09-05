export class Stream {
    name: string;
    description: string;
    service: string;
    'stream-type': string;
    tags: [string];
    partitions: number;
    generator: {
       'generator-type': string;
        service: string;
        'instance-count': number;
    };
    'replication-factor': number;
}
