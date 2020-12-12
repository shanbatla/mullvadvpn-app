#pragma once

#include <windows.h>
#include <string>
#include <optional>
#include <setupapi.h>

struct DeviceInfo
{
	HDEVINFO deviceInfoSet;
	SP_DEVINFO_DATA deviceInfo;
};

//
// Generic functions
//

std::wstring
GetDeviceStringProperty
(
	HDEVINFO deviceInfoSet,
	const SP_DEVINFO_DATA &deviceInfo,
	const DEVPROPKEY *property
);

void
CreateDevice
(
	const GUID &classGuid,
	const std::wstring &deviceName,
	const std::wstring &deviceHardwareId
);

void
InstallDriverForDevice
(
	const std::wstring &deviceHardwareId,
	const std::wstring &infPath
);

std::optional<DeviceInfo>
FindFirstDevice
(
	const GUID *deviceClass,
	const std::wstring &deviceName
);

void
UninstallDevice
(
	const DeviceInfo &deviceInfo
);

//
// Functions that are specific to our driver/implementation
//

HANDLE
OpenSplitTunnelDevice
(
);

void
CloseSplitTunnelDevice
(
	HANDLE device
);

void
SendIoControl
(
	HANDLE device,
	DWORD code,
	void *inBuffer,
	DWORD inBufferSize,
	void *outBuffer,
	DWORD outBufferSize,
	DWORD *bytesReturned
);

void
SendIoControlReset
(
	HANDLE device
);
