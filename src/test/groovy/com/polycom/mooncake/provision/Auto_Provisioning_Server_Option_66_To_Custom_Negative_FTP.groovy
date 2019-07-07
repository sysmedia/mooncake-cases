package com.polycom.mooncake.provision


import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
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
 * Created by taochen on 2019-06-13.
 *
 * Provisioning mode: DHCP auto_66
 * Provisioning profile:
 * <ProvDHCPOpt>60</ProvDHCPOpt>
 * Provisioning server: ftp
 * MoonCake set to manual ftp provisioning.
 * MoonCake won't accept new DHCP option when reprovisioning interval
 * MoonCake won't accept new DHCP option when reboot
 */
class Auto_Provisioning_Server_Option_66_To_Custom_Negative_FTP extends MoonCakeProvisionTestSpec {
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

    @Shared
    String gs_sip_username

    @Shared
    String mc_sip_username

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        def mcDialString = generateDialString(moonCake)
        gs_e164Num = gsDialString.e164Number
        gs_h323Name = gsDialString.h323Name
        gs_sip_username = gsDialString.sipUri
        mc_e164Num = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_sip_username = mcDialString.sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
    }

    def "Verify if the MoonCake will not accept settings from DHCP options when it is set as FTP provisioning"() {
        def attributesToBeSet = [provision: [
                'profileUpdateCheckInterval': 'PT60S',
                'ProvDHCPOpt'               : '60',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TCP',
                'enableH323'                : 'disable']
        ]

        String newMcSipUri = generateDialString(moonCake).sipUri
        def attributesToBeSet60 = [
                provision: [
                        'profileUpdateCheckInterval': 'PT60S',
                        'ProvDHCPOpt'               : '60',
                        'enableSIP'                 : 'enable',
                        'sipProxyServer'            : dma.ip,
                        'sipRegistrarServer'        : dma.ip,
                        'userName'                  : newMcSipUri,
                        'transport'                 : 'TCP',
                        'enableH323'                : 'disable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        FtpClient ftpClient60 = new FtpClient(FTP_SERVER, FTP_OPTION60_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)
        deleteConfigOnFtp(ftpClient60, mac)
        createConfigOnFtp(ftpClient60, mac)

        then: "Update the DHCH settings for later provisioning"
        doCommandOnHttp(DHCP_SERVER, cmdDel66)
        doCommandOnHttp(DHCP_SERVER, cmdDel60)
        doCommandOnHttp(DHCP_SERVER, cmdDel160)
        doCommandOnHttp(DHCP_SERVER, cmdFtpSet66)
        doCommandOnHttp(DHCP_SERVER, cmdFtpSet60)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSet)
        modifyConfigOnFtp(ftpClient60, mac, attributesToBeSet60)
        pauseTest(5)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)
        pauseTest(10)

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

        then: "MoonCake place H323 call with the GS"
        moonCake.placeCall(groupSeries, CallType.SIP, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        then: "Wait 2 minutes for the next re-provision interval"
        pauseTest(120)
        retry(times: 3, delay: 60) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert !dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
            assert !dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(newMcSipUri)
        }

        then: "Hang up the call and reboot the MoonCake"
        moonCake.hangUp()
        moonCake.reboot()
        pauseTest(120)

        then: "Verify if the settings from DHCP option 60 has not been retrieved by the MoonCake"
        retry(times: 3, delay: 10) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert !dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
            assert !dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(newMcSipUri)
        }

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        doCommandOnHttp(DHCP_SERVER, cmdDel60)
    }
}
