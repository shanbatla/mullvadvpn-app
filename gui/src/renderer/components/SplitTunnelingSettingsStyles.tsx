import styled from 'styled-components';
import { colors } from '../../config.json';
import * as AppButton from './AppButton';
import * as Cell from './cell';
import { Container } from './Layout';
import { NavigationScrollbars } from './NavigationBar';

export const StyledPageCover = styled.div({}, (props: { show: boolean }) => ({
  position: 'absolute',
  zIndex: 2,
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: '#000000',
  opacity: 0.6,
  display: props.show ? 'block' : 'none',
}));

export const StyledContainer = styled(Container)({
  backgroundColor: colors.darkBlue,
});

export const StyledNavigationScrollbars = styled(NavigationScrollbars)({
  flex: 1,
});

export const StyledContent = styled.div({
  display: 'flex',
  flexDirection: 'column',
  flex: 1,
});

export const StyledCellButton = styled(Cell.CellButton)((props: { lookDisabled?: boolean }) => ({
  ':not(:disabled):hover': {
    backgroundColor: props.lookDisabled ? colors.blue : undefined,
  },
}));

const disabledApplication = (props: { lookDisabled?: boolean }) => ({
  opacity: props.lookDisabled ? 0.6 : undefined,
});

export const StyledIcon = styled(Cell.UntintedIcon)(disabledApplication, {
  marginRight: '12px',
});

export const StyledCellLabel = styled(Cell.Label)(disabledApplication, {
  fontFamily: 'Open Sans',
  fontWeight: 'normal',
  fontSize: '16px',
});

export const StyledIconPlaceholder = styled.div({
  width: '35px',
  marginRight: '12px',
});

export const StyledApplicationListContent = styled.div({
  display: 'flex',
  flexDirection: 'column',
});

export const StyledApplicationListAnimation = styled.div({}, (props: { height?: number }) => ({
  overflow: 'hidden',
  height: props.height ? `${props.height}px` : 'auto',
  transition: 'height 500ms ease-in-out',
  marginBottom: '20px',
}));

export const StyledSpinnerRow = styled.div({
  display: 'flex',
  justifyContent: 'center',
  padding: '8px 0',
  background: colors.blue40,
});

export const StyledBrowseButton = styled(AppButton.BlueButton)({
  margin: '0 22px 22px',
});

export const StyledCellContainer = styled(Cell.Container)({
  marginBottom: '20px',
});
