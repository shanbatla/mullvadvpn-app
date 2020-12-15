type Warning = 'launches-in-existing-process' | 'launches-elsewhere';

export interface ISplitTunnelingApplication {
  path: string;
  name: string;
  icon?: string;
}

export interface ILinuxSplitTunnelingApplication extends ISplitTunnelingApplication {
  exec: string;
  warning?: Warning;
}
