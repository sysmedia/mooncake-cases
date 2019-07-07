package com.polycom.mooncake.Interop.FTP

import com.polycom.api.rest.plcm_h323_alias_type.PlcmH323AliasType
import com.polycom.api.rest.plcm_h323_identity.H323RegistrationState
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec
import spock.lang.Shared

/**
 * Created by taochen on 2019-06-22.
 *
 * Provisioning server: FTP
 * Provision items: call setting H323/SIP AES provisioning items.
 * <CallSettings>
 * 		<aesEncryption>auto</aesEncryption>     // value: auto\off\on
 * 		<natConfig>off</natConfig>              // value: on\off
 * 		<natWANAddress></natWANAddress>         // nat WAN Address
 * 		<networkCallRate>4096</networkCallRate> // value: 64\256\384\512\768\1024\1537\2048\3072\4096
 * 		<tcpStartPort>3230</tcpStartPort>       // value: defaut 3030
 * 		<tcpEndPort>3250</tcpEndPort>           // value: tcpStartPort + 20
 * 		<udpStartPort>3230</udpStartPort>       // value: defaut 3030
 * 		<udpEndPort>3250</udpEndPort>           // value: udpStartPort + 20
 * 		<useFixedPorts>false</useFixedPorts>    // value: true\false
 * </CallSettings>
 */
class FTP_Provisioning_Call_Settings_AES_Settings_H323 extends MoonCakeProvisionTestSpec {
    @Shared
    GroupSeries groupSeries

    @Shared
    Dma dma

    @Shared
    String gs_e164Num

    @Shared
    String gs_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String mc_h323Name

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def mcDialString = generateDialString(moonCake)
        gs_e164Num = gsDialString.e164Number
        gs_h323Name = gsDialString.h323Name
        mc_e164Num = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
    }

    def cleanupSpec() {
        groupSeries.setEncryption("no")
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
    }

    def "Verify if the MoonCake can be provisioned by the FTPS with the AES settings"() {
        def attributesToBeSetAesAuto = [provision: [
                'aesEncryption'    : 'auto',
                'enableSIP'        : 'disable',
                'e164Number'       : mc_e164Num,
                'h323alias'        : mc_h323Name,
                'gatekeeperAddress': dma.ip,
                'h323AuthEnabled'  : 'false',
                'enableH323'       : 'enable']
        ]

        def attributesToBeSetAesDisable = [provision: [
                'aesEncryption'    : 'off',
                'enableSIP'        : 'disable',
                'e164Number'       : mc_e164Num,
                'h323alias'        : mc_h323Name,
                'gatekeeperAddress': dma.ip,
                'h323AuthEnabled'  : 'false',
                'enableH323'       : 'enable']
        ]

        def attributesToBeSetAesEnable = [provision: [
                'aesEncryption'    : 'on',
                'enableSIP'        : 'disable',
                'e164Number'       : mc_e164Num,
                'h323alias'        : mc_h323Name,
                'gatekeeperAddress': dma.ip,
                'h323AuthEnabled'  : 'false',
                'enableH323'       : 'enable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpsClient = new FtpClient(FTP_SERVER, FTPS_USER, FTPS_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpsClient, mac)
        createConfigOnFtp(ftpsClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpsClient, mac, attributesToBeSetAesAuto)
        pauseTest(5)

        then: "MoonCake provision onto the DHCP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTPS, 66, FTP_SERVER, "", FTPS_USER, FTPS_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTPS Server"
        retry(times: 3, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == mc_e164Num
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_ID
            }.value == mc_h323Name
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)
        groupSeries.setEncryption("yes")
        pauseTest(5)

        then: "MoonCake place H323 call with the GS"
        groupSeries.placeCall(mc_e164Num, CallType.H323, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:true")

        then: "Hang up the call"
        moonCake.hangUp()
        pauseTest(2)

        then: "Disable the encryption in the remote endpoint"
        groupSeries.setEncryption("no")

        then: "Place a H323 call again"
        groupSeries.placeCall(mc_e164Num, CallType.H323, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:false")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:false")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:false")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:false")

        then: "Hang up the call"
        moonCake.hangUp()
        pauseTest(2)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpsClient, mac, attributesToBeSetAesDisable)
        pauseTest(5)

        then: "MoonCake provision onto the DHCP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTPS, 66, FTP_SERVER, "", FTPS_USER, FTPS_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTPS Server"
        retry(times: 3, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == mc_e164Num
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_ID
            }.value == mc_h323Name
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)
        groupSeries.setEncryption("yes")
        pauseTest(5)

        then: "MoonCake place H323 call with the GS"
        groupSeries.placeCall(mc_e164Num, CallType.H323, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:false")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:false")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:false")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:false")

        then: "Hang up the call"
        moonCake.hangUp()
        pauseTest(2)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpsClient, mac, attributesToBeSetAesEnable)
        pauseTest(5)

        then: "MoonCake provision onto the DHCP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTPS, 66, FTP_SERVER, "", FTPS_USER, FTPS_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTPS Server"
        retry(times: 3, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_DIALDIGITS
            }.value == mc_e164Num
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmH323Identity != null &&
                        x.plcmH323Identity.h323RegistrationState == H323RegistrationState.ACTIVE
            }.plcmH323Identity.h323Alias.find { y ->
                y.plcmH323AliasType == PlcmH323AliasType.H323_ID
            }.value == mc_h323Name
        }

        when: "Provision the GS onto the GK server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)
        pauseTest(5)

        then: "MoonCake place H323 call with the GS"
        groupSeries.placeCall(mc_e164Num, CallType.H323, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:true")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
    }
}
