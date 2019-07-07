package com.polycom.mooncake.Interop.FTP


import com.polycom.api.rest.plcm_sip_identity_v2.SipRegistrationState
import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.ServiceStatus
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.moonCake.enums.ProvisioningMode
import com.polycom.mooncake.MoonCakeProvisionTestSpec
import spock.lang.Shared
import spock.lang.Unroll

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
class FTP_Provisioning_Call_Settings_AES_Settings_SIP extends MoonCakeProvisionTestSpec {
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

    @Shared
    def attributesToBeSetAesAutoTcp

    @Shared
    def attributesToBeSetAesAutoUdp

    @Shared
    def attributesToBeSetAesAutoTls

    @Shared
    def attributesToBeSetAesOnTcp

    @Shared
    def attributesToBeSetAesOnUdp

    @Shared
    def attributesToBeSetAesOnTls

    @Shared
    def attributesToBeSetAesOffTcp

    @Shared
    def attributesToBeSetAesOffUdp

    @Shared
    def attributesToBeSetAesOffTls

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

        attributesToBeSetAesAutoTcp = [provision: [
                'aesEncryption'             : 'auto',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TCP',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesAutoUdp = [provision: [
                'aesEncryption'             : 'auto',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'UDP',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesAutoTls = [provision: [
                'aesEncryption'             : 'auto',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TLS',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesOnTcp = [provision: [
                'aesEncryption'             : 'on',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TCP',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesOnUdp = [provision: [
                'aesEncryption'             : 'on',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'UDP',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesOnTls = [provision: [
                'aesEncryption'             : 'on',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TLS',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesOffTcp = [provision: [
                'aesEncryption'             : 'off',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TCP',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesOffUdp = [provision: [
                'aesEncryption'             : 'off',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'UDP',
                'enableH323'                : 'disable']
        ]

        attributesToBeSetAesOffTls = [provision: [
                'aesEncryption'             : 'off',
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TLS',
                'enableH323'                : 'disable']
        ]
    }

    def cleanupSpec() {
        groupSeries.setEncryption("no")
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
    }

    @Unroll
    def "Verify if the MoonCake can be provisioned by the FTPS with the AES #aes and #transport transport protocol"(String aes,
                                                                                                                    String transport,
                                                                                                                    Map config) {

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpsClient = new FtpClient(FTP_SERVER, FTPS_USER, FTPS_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpsClient, mac)
        createConfigOnFtp(ftpsClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpsClient, mac, config)
        pauseTest(5)

        then: "MoonCake provision with TCP AES auto"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTPS, 66, FTP_SERVER, "", FTPS_USER, FTPS_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTPS Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
        }

        when: "Provision the GS onto the SIP server with auto TLS transport protocol"
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_AUTO)
        groupSeries.setEncryption("yes")
        pauseTest(5)

        then: "MoonCake place SIP call with the GS"
        moonCake.placeCall(gs_sip_username, CallType.SIP, 2048)

        then: "Verify the media statistics during the call"
        if (aes == "auto" || aes == "on") {
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:true")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:true")
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:true")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:true")
        } else {
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:false")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:false")
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:false")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:false")
        }

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        pauseTest(2)

        where:
        aes    | transport | config
        "auto" | "TCP"     | attributesToBeSetAesAutoTcp
        "auto" | "UDP"     | attributesToBeSetAesAutoUdp
        "auto" | "TLS"     | attributesToBeSetAesAutoTls
        "on"   | "TCP"     | attributesToBeSetAesOnTcp
        "on"   | "UDP"     | attributesToBeSetAesOnUdp
        "on"   | "TLS"     | attributesToBeSetAesOnTls
        "off"  | "TCP"     | attributesToBeSetAesOffTcp
        "off"  | "UDP"     | attributesToBeSetAesOffUdp
        "off"  | "TLS"     | attributesToBeSetAesOffTls
    }
}
