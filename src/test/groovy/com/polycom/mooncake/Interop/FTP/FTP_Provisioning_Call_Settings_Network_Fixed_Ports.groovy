package com.polycom.mooncake.Interop.FTP

import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-06-22.
 *
 * Provisioning server: FTP
 * Provision items: call setting fixed ports provisioning items.
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
class FTP_Provisioning_Call_Settings_Network_Fixed_Ports extends MoonCakeProvisionTestSpec {
    @Shared
    String mc_h323Name

    @Shared
    String mc_e164Num

    @Shared
    String mc_sip_username

    @Shared
    Dma dma

    @Shared
    def attributesToBeSetForFtpFixPortTrue

    @Shared
    def attributesToBeSetForFtpFixPortFalse

    def setupSpec() {
        dma = testContext.bookSut(Dma.class, keyword)
        def mcDialString = generateDialString(moonCake)
        mc_e164Num = mcDialString.e164Number
        mc_h323Name = mcDialString.h323Name
        mc_sip_username = mcDialString.sipUri

        attributesToBeSetForFtpFixPortTrue = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'useFixedPorts'             : 'true',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : "TCP",
                'enableH323'                : 'disable']
        ]

        attributesToBeSetForFtpFixPortFalse = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'useFixedPorts'             : 'false',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : "TCP",
                'enableH323'                : 'disable']
        ]
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
    }

    @Unroll
    def "Verify if the MoonCake can be successfully provisioned by the FTP with the #fixedOrNot fixed port"(String fixedOrNot, Map config) {


        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, config)
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

        where:
        fixedOrNot | config
        "true"     | attributesToBeSetForFtpFixPortTrue
        "false"    | attributesToBeSetForFtpFixPortFalse
    }
}
