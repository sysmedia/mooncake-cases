package com.polycom.mooncake.Interop.FTP

import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.moonCake.enums.H460FirewallTraversal
import com.polycom.honeycomb.moonCake.enums.NatMode
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-06-22.
 *
 * Provisioning server: FTP
 * Provision items: call setting natConfig/natWANAddress provisioning items.
 * <CallSettings>
 * 		<aesEncryption>auto</aesEncryption>     // value: auto\disable\enable
 * 		<natConfig>off</natConfig>              // value: auto\on\off
 * 		<natWANAddress></natWANAddress>         // nat WAN Address
 * 		<networkCallRate>4096</networkCallRate> // value: 64\256\384\512\768\1024\1537\2048\3072\4096
 * 		<tcpStartPort>3230</tcpStartPort>       // value: defaut 3030
 * 		<tcpEndPort>3250</tcpEndPort>           // value: tcpStartPort + 20
 * 		<udpStartPort>3230</udpStartPort>       // value: defaut 3030
 * 		<udpEndPort>3250</udpEndPort>           // value: udpStartPort + 20
 * 		<useFixedPorts>false</useFixedPorts>    // value: true\false
 * </CallSettings>
 */
class FTP_Provisioning_Call_Settings_NAT_Config extends MoonCakeProvisionTestSpec {
    @Shared
    String mc_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String mc_sip_username

    @Shared
    Dma dma

    def setupSpec() {
        dma = testContext.bookSut(Dma.class, keyword)
        def mcDialString = generateDialString(moonCake)
        mc_e164Num = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_sip_username = mcDialString.sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        moonCake.updateProvisioningSettings(ProvisioningMode.DISABLE, 66, "", "", "", "")
        moonCake.updateNatSetting(NatMode.DISABLE, H460FirewallTraversal.DISABLE,
                "", 3230, 3232, 3230, 3238)
    }

    @Unroll
    def "Verify if the MoonCake can be successfully provisioned by the FTP with the NAT settings"() {
        def attributesToBeSetForFtpNatOn = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'natConfig'                 : 'on',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : "TCP",
                'enableH323'                : 'disable']
        ]

        def attributesToBeSetForFtpNatAuto = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'natConfig'                 : 'auto',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : "TCP",
                'enableH323'                : 'disable']
        ]

        def attributesToBeSetForFtpNatOff = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'natConfig'                 : 'off',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : "TCP",
                'enableH323'                : 'disable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSetForFtpNatOff)
        pauseTest(5)

        when: "Disable the provision on the MoonCake"
        moonCake.updateProvisioningSettings(ProvisioningMode.DISABLE, 66, "", "", "", "")

        then: "Update the NAT settings to open the firewall traversal"
        moonCake.updateNatSetting(NatMode.MANUAL,
                H460FirewallTraversal.ENABLE,
                "10.10.10.10",
                3230,
                3232,
                3230,
                3238)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
        }

//        then: "Verify if the firewall traversal function is disabled by the FTP provision"
//        assert moonCake.natSettings.fireWallSettings.natConfig == NatMode.DISABLE.mode

        when: "Disable the firewall traversal function on the MoonCake"
        moonCake.updateProvisioningSettings(ProvisioningMode.DISABLE, 66, "", "", "", "")
        moonCake.updateNatSetting(NatMode.DISABLE, H460FirewallTraversal.DISABLE,
                "", 3230, 3232, 3230, 3238)

        and: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSetForFtpNatOn)
        pauseTest(5)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
        }

//        then: "Verify if the firewall traversal function is enabled by the FTP provision"
//        assert moonCake.natSettings.fireWallSettings.natConfig == NatMode.DISABLE.mode

        when: "Disable the firewall traversal function on the MoonCake"
        moonCake.updateProvisioningSettings(ProvisioningMode.DISABLE, 66, "", "", "", "")
        moonCake.updateNatSetting(NatMode.DISABLE, H460FirewallTraversal.DISABLE,
                "", 3230, 3232, 3230, 3238)

        and: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSetForFtpNatAuto)
        pauseTest(5)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
        }

//        then: "Verify if the firewall traversal function is enabled by the FTP provision"
//        assert moonCake.natSettings.fireWallSettings.natConfig == NatMode.AUTO.mode
    }
}
