import { app, shell } from 'electron';
import fs from 'fs';
import path from 'path';
import { ISplitTunnelingApplication } from '../shared/split-tunneling-application';

const APPLICATION_PATHS = [
  `${process.env.ProgramData}/Microsoft/Windows/Start Menu/Programs`,
  `${process.env.AppData}/Microsoft/Windows/Start Menu/Programs`,
];

interface ShortcutDetails {
  target: string;
  name: string;
}

// Finds applications by searching through the startmenu for shortcuts with and exe-file as target.
export async function getApplications(
  applicationPaths?: string[],
): Promise<ISplitTunnelingApplication[]> {
  const links = await Promise.all(APPLICATION_PATHS.map(findAllLinks));
  let shortcuts = resolveLinks(links.flat());

  if (applicationPaths) {
    const startMenuApplications = shortcuts.filter((shortcut) =>
      applicationPaths.includes(shortcut.target),
    );

    const nonStartMenuApplications = applicationPaths
      .filter(
        (applicationPath) => !shortcuts.some((shortcut) => shortcut.target === applicationPath),
      )
      .map((applicationPath) => ({
        target: applicationPath,
        name: path.basename(applicationPath),
      }));

    shortcuts = [...startMenuApplications, ...nonStartMenuApplications];
  }

  return convertToSplitTunnelingApplications(shortcuts);
}

async function findAllLinks(path: string): Promise<string[]> {
  if (path.endsWith('.lnk')) {
    return [path];
  } else {
    const stat = await fs.promises.stat(path);
    if (stat.isDirectory()) {
      const contents = await fs.promises.readdir(path);
      const result = await Promise.all(contents.map((item) => findAllLinks(`${path}/${item}`)));
      return result.flat();
    } else {
      return [];
    }
  }
}

function resolveLinks(linkPaths: string[]): ShortcutDetails[] {
  return linkPaths
    .map((link) => {
      try {
        return {
          ...shell.readShortcutLink(path.resolve(link)),
          name: path.parse(link).name,
        };
      } catch (_e) {
        return null;
      }
    })
    .filter(
      (shortcut): shortcut is ShortcutDetails =>
        shortcut !== null &&
        shortcut.target.endsWith('.exe') &&
        !shortcut.target.toLowerCase().includes('uninstall') &&
        !shortcut.name.toLowerCase().includes('uninstall'),
    );
}

function convertToSplitTunnelingApplications(
  shortcuts: ShortcutDetails[],
): Promise<ISplitTunnelingApplication[]> {
  return Promise.all(
    shortcuts.map(async (shortcut) => {
      return {
        path: shortcut.target,
        name: shortcut.name,
        icon: await retrieveIcon(shortcut.target),
      };
    }),
  );
}

async function retrieveIcon(exe: string) {
  const icon = await app.getFileIcon(exe);
  return icon.toDataURL();
}
