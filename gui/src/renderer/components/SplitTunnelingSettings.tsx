import { remote } from 'electron';
import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { sprintf } from 'sprintf-js';
import { colors } from '../../config.json';
import { messages } from '../../shared/gettext';
import {
  ISplitTunnelingApplication,
  ILinuxSplitTunnelingApplication,
} from '../../shared/split-tunneling-application';
import consumePromise from '../../shared/promise';
import { useAppContext } from '../context';
import { IReduxState } from '../redux/store';
import Accordion from './Accordion';
import * as AppButton from './AppButton';
import * as Cell from './cell';
import ImageView from './ImageView';
import { Layout } from './Layout';
import { ModalContainer, ModalAlert, ModalAlertType } from './Modal';
import {
  BackBarItem,
  NavigationBar,
  NavigationContainer,
  NavigationItems,
  TitleBarItem,
} from './NavigationBar';
import SettingsHeader, { HeaderSubTitle, HeaderTitle } from './SettingsHeader';
import {
  StyledPageCover,
  StyledContainer,
  StyledNavigationScrollbars,
  StyledContent,
  StyledCellButton,
  StyledIcon,
  StyledCellLabel,
  StyledIconPlaceholder,
  StyledApplicationListContent,
  StyledApplicationListAnimation,
  StyledSpinnerRow,
  StyledBrowseButton,
  StyledCellContainer,
} from './SplitTunnelingSettingsStyles';

export default function SplitTunneling() {
  const { goBack } = useHistory();
  const [browsing, setBrowsing] = useState(false);

  return (
    <>
      <StyledPageCover show={browsing} />
      <ModalContainer>
        <Layout>
          <StyledContainer>
            <NavigationContainer>
              <NavigationBar>
                <NavigationItems>
                  <BackBarItem action={goBack}>
                    {
                      // TRANSLATORS: Back button in navigation bar
                      messages.pgettext('navigation-bar', 'Advanced')
                    }
                  </BackBarItem>
                  <TitleBarItem>
                    {
                      // TRANSLATORS: Title label in navigation bar
                      messages.pgettext('split-tunneling-nav', 'Split tunneling')
                    }
                  </TitleBarItem>
                </NavigationItems>
              </NavigationBar>

              <StyledNavigationScrollbars>
                <StyledContent>
                  <PlatformSpecificSplitTunnelingSettings setBrowsing={setBrowsing} />
                </StyledContent>
              </StyledNavigationScrollbars>
            </NavigationContainer>
          </StyledContainer>
        </Layout>
      </ModalContainer>
    </>
  );
}
interface IPlatformSplitTunnelingSettingsProps {
  setBrowsing: (value: boolean) => void;
}

function PlatformSpecificSplitTunnelingSettings(props: IPlatformSplitTunnelingSettingsProps) {
  switch (process.platform) {
    case 'linux':
      return <LinuxSplitTunnelingSettings {...props} />;
    case 'win32':
      return <WindowsSplitTunnelingSettings {...props} />;
    default:
      throw new Error(`Split tunneling not implemented on ${process.platform}`);
  }
}

function useFilePicker(
  buttonLabel: string,
  setOpen: (value: boolean) => void,
  select: (path: string) => void,
) {
  return useCallback(async () => {
    setOpen(true);
    const file = await remote.dialog.showOpenDialog({ properties: ['openFile'], buttonLabel });
    setOpen(false);

    if (file.filePaths[0]) {
      select(file.filePaths[0]);
    }
  }, [buttonLabel, setOpen, select]);
}

function LinuxSplitTunnelingSettings(props: IPlatformSplitTunnelingSettingsProps) {
  const { getLinuxSplitTunnelingApplications, launchExcludedApplication } = useAppContext();

  const [applications, setApplications] = useState<ILinuxSplitTunnelingApplication[]>();
  useEffect(() => consumePromise(getLinuxSplitTunnelingApplications().then(setApplications)), []);

  const launchWithFilePicker = useFilePicker(
    messages.pgettext('split-tunneling-view', 'Launch'),
    props.setBrowsing,
    launchExcludedApplication,
  );

  return (
    <>
      <SettingsHeader>
        <HeaderTitle>{messages.pgettext('split-tunneling-view', 'Split tunneling')}</HeaderTitle>
        <HeaderSubTitle>
          {messages.pgettext(
            'split-tunneling-view',
            'Click on an app to launch it. Its traffic will bypass the VPN tunnel until you close it.',
          )}
        </HeaderSubTitle>
      </SettingsHeader>

      <ApplicationList
        applications={applications}
        onSelect={launchExcludedApplication}
        rowComponent={LinuxApplicationRow}
      />

      <StyledBrowseButton onClick={launchWithFilePicker}>
        {messages.pgettext('split-tunneling-view', 'Browse')}
      </StyledBrowseButton>
    </>
  );
}

interface ILinuxApplicationRowProps {
  application: ILinuxSplitTunnelingApplication;
  onSelect?: (application: ILinuxSplitTunnelingApplication) => void;
}

function LinuxApplicationRow(props: ILinuxApplicationRowProps) {
  const [showWarning, setShowWarning] = useState(false);

  const launch = useCallback(() => {
    setShowWarning(false);
    props.onSelect?.(props.application);
  }, [props.onSelect, props.application]);

  const showWarningDialog = useCallback(() => setShowWarning(true), []);
  const hideWarningDialog = useCallback(() => setShowWarning(false), []);

  const disabled = props.application.warning === 'launches-elsewhere';
  const warningColor = disabled ? colors.red : colors.yellow;
  const warningMessage = disabled
    ? sprintf(
        messages.pgettext(
          'split-tunneling-view',
          '%(applicationName)s is problematic and can’t be excluded from the VPN tunnel.',
        ),
        {
          applicationName: props.application.name,
        },
      )
    : sprintf(
        messages.pgettext(
          'split-tunneling-view',
          'If it’s already running, close %(applicationName)s before launching it from here. Otherwise it might not be excluded from the VPN tunnel.',
        ),
        {
          applicationName: props.application.name,
        },
      );
  const warningDialogButtons = disabled
    ? [
        <AppButton.BlueButton key="cancel" onClick={hideWarningDialog}>
          {messages.gettext('Back')}
        </AppButton.BlueButton>,
      ]
    : [
        <AppButton.BlueButton key="launch" onClick={launch}>
          {messages.pgettext('split-tunneling-view', 'Launch')}
        </AppButton.BlueButton>,
        <AppButton.BlueButton key="cancel" onClick={hideWarningDialog}>
          {messages.gettext('Cancel')}
        </AppButton.BlueButton>,
      ];

  return (
    <>
      <StyledCellButton
        onClick={props.application.warning ? showWarningDialog : launch}
        lookDisabled={disabled}>
        {props.application.icon ? (
          <StyledIcon
            source={props.application.icon}
            width={35}
            height={35}
            lookDisabled={disabled}
          />
        ) : (
          <StyledIconPlaceholder />
        )}
        <StyledCellLabel lookDisabled={disabled}>{props.application.name}</StyledCellLabel>
        {props.application.warning && <Cell.Icon source="icon-alert" tintColor={warningColor} />}
      </StyledCellButton>
      {showWarning && (
        <ModalAlert
          type={ModalAlertType.warning}
          iconColor={warningColor}
          message={warningMessage}
          buttons={warningDialogButtons}
          close={hideWarningDialog}
        />
      )}
    </>
  );
}

export function WindowsSplitTunnelingSettings(props: IPlatformSplitTunnelingSettingsProps) {
  const {
    addSplitTunnelingApplication,
    removeSplitTunnelingApplication,
    getWindowsSplitTunnelingApplications,
    setSplitTunnelingState,
  } = useAppContext();
  const splitTunnelingEnabled = useSelector((state: IReduxState) => state.settings.splitTunneling);
  const splitTunnelingApplications = useSelector(
    (state: IReduxState) => state.settings.splitTunnelingApplications,
  );

  const [applications, setApplications] = useState<ISplitTunnelingApplication[]>();
  useEffect(() => consumePromise(getWindowsSplitTunnelingApplications().then(setApplications)), []);

  const addWithFilePicker = useFilePicker(
    messages.pgettext('split-tunneling-view', 'Add'),
    props.setBrowsing,
    addSplitTunnelingApplication,
  );

  return (
    <>
      <SettingsHeader>
        <HeaderTitle>{messages.pgettext('split-tunneling-view', 'Split tunneling')}</HeaderTitle>
        <HeaderSubTitle>
          {messages.pgettext(
            'split-tunneling-view',
            'Split tunneling makes it possible to select which applications should not be routed through the VPN tunnel.',
          )}
        </HeaderSubTitle>
      </SettingsHeader>

      <StyledCellContainer>
        <Cell.Label>{messages.pgettext('split-tunneling-view', 'Enabled')}</Cell.Label>
        <Cell.Switch isOn={splitTunnelingEnabled} onChange={setSplitTunnelingState} />
      </StyledCellContainer>

      <Accordion expanded={true}>
        <Cell.Section>
          <Cell.SectionTitle>
            {messages.pgettext('split-tunneling-view', 'Excluded applications')}
          </Cell.SectionTitle>
          <ApplicationList
            applications={splitTunnelingApplications}
            onRemove={removeSplitTunnelingApplication}
            rowComponent={ApplicationRow}
          />
        </Cell.Section>

        <Cell.Section>
          <Cell.SectionTitle>
            {messages.pgettext('split-tunneling-view', 'Add applications')}
          </Cell.SectionTitle>
          <ApplicationList
            applications={applications}
            onSelect={addSplitTunnelingApplication}
            rowComponent={ApplicationRow}
          />
        </Cell.Section>

        <StyledBrowseButton onClick={addWithFilePicker}>
          {messages.pgettext('split-tunneling-view', 'Browse')}
        </StyledBrowseButton>
      </Accordion>
    </>
  );
}

interface IApplicationListProps<T extends ISplitTunnelingApplication> {
  applications: T[] | undefined;
  onSelect?: (application: T) => void;
  onRemove?: (application: T) => void;
  rowComponent: React.ComponentType<IApplicationRowProps<T>>;
}

function ApplicationList<T extends ISplitTunnelingApplication>(props: IApplicationListProps<T>) {
  const [applicationListHeight, setApplicationListHeight] = useState<number>();
  const applicationListRef = useRef() as React.RefObject<HTMLDivElement>;

  useLayoutEffect(() => {
    const height = applicationListRef.current?.getBoundingClientRect().height;
    setApplicationListHeight(height);
  }, [props.applications]);

  return (
    <StyledApplicationListAnimation height={applicationListHeight}>
      <StyledApplicationListContent ref={applicationListRef}>
        {props.applications === undefined ? (
          <StyledSpinnerRow>
            <ImageView source="icon-spinner" height={60} width={60} />
          </StyledSpinnerRow>
        ) : (
          props.applications.map((application) => (
            <props.rowComponent
              key={application.path}
              application={application}
              onSelect={props.onSelect}
              onRemove={props.onRemove}
            />
          ))
        )}
      </StyledApplicationListContent>
    </StyledApplicationListAnimation>
  );
}

interface IApplicationRowProps<T extends ISplitTunnelingApplication> {
  application: T;
  onSelect?: (application: T) => void;
  onRemove?: (application: T) => void;
}

function ApplicationRow<T extends ISplitTunnelingApplication>(props: IApplicationRowProps<T>) {
  const onSelect = useCallback(() => {
    props.onSelect?.(props.application);
  }, [props.onSelect, props.application]);

  const onRemove = useCallback(() => {
    props.onRemove?.(props.application);
  }, [props.onRemove, props.application]);

  return (
    <Cell.CellButton onClick={props.onSelect ? onSelect : undefined}>
      {props.application.icon ? (
        <StyledIcon source={props.application.icon} width={35} height={35} />
      ) : (
        <StyledIconPlaceholder />
      )}
      <StyledCellLabel>{props.application.name}</StyledCellLabel>
      {props.onRemove && (
        <ImageView
          source="icon-close"
          width={16}
          height={16}
          onClick={onRemove}
          tintColor={colors.white60}
          tintHoverColor={colors.white80}
        />
      )}
    </Cell.CellButton>
  );
}
