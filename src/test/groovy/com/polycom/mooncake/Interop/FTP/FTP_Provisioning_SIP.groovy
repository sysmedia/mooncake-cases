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
 * Created by taochen on 2019-06-18.
 *
 * Provisioning server: FTPS
 * Provision items: SIP related value
 * <SIP>
 * 		<authorizationName>soakuser</authorizationName>         // auth name
 * 		<domain></domain>                                       // domain
 * 		<enableSIP>enable</enableSIP>                           // value: enable\DISABLED
 * 		<password>soakpass</password>                           // password
 * 		<sipProxyServer>172.21.105.171</sipProxyServer>         // sip proxy perver
 * 		<sipRegistrarServer>172.21.105.171</sipRegistrarServer> // sip registrar server
 * 		<sipServerType>standard</sipServerType>                 // standard\
 * 		<transport>UDP</transport>                              // TCP\UDP\TLS
 * 		<userName>$SUT_sip</userName>                           // sip number
 * </SIP>
 */
class FTP_Provisioning_SIP extends MoonCakeProvisionTestSpec {
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
    String displayName = "Poly Group 150"

    @Shared
    String authName = "mooncake1"

    @Shared
    String authPasswd = "polycom"

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

    @Unroll
    def "Verify if the MoonCake can be successfully provisioned by the FTPS with SIP transport protocol #mcSipProtocol"(String mcSipProtocol,
                                                                                                                        SipTransportProtocol gsSipProtocol) {
        def attributesToBeSetForFtps = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'displayName'               : 'Poly Group 150',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : mcSipProtocol,
                'enableH323'                : 'disable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpsClient = new FtpClient(FTP_SERVER, FTPS_USER, FTPS_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpsClient, mac)
        createConfigOnFtp(ftpsClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpsClient, mac, attributesToBeSetForFtps)
        pauseTest(5)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTPS, 66, FTP_SERVER, "", FTPS_USER, FTPS_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the FTP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert moonCake.registerInfo.username == displayName
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip, gsSipProtocol)

        then: "MoonCake place H323 call with the GS"
        moonCake.placeCall(gs_sip_username, CallType.SIP, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()

        where:
        mcSipProtocol | gsSipProtocol
        "TCP"         | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP
        "UCP"         | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_UDP
        "TLS"         | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS
    }

    @Unroll
    def "Verify if the MoonCake can be provisioned by the FTPS with SIP transport protocol #mcSipProtocol in registration authentication setting"(String mcSipProtocol,
                                                                                                                                                  SipTransportProtocol gsSipProtocol) {
        setup:
        dma = testContext.bookSut(Dma.class, "Auth")

        def attributesToBeSetForFtps = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'displayName'               : 'Poly Group 150',
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : mcSipProtocol,
                'enableH323'                : 'disable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpsClient = new FtpClient(FTP_SERVER, FTPS_USER, FTPS_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpsClient, mac)
        createConfigOnFtp(ftpsClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpsClient, mac, attributesToBeSetForFtps)
        pauseTest(5)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTPS, 66, FTP_SERVER, "", FTPS_USER, FTPS_PASSWORD)
        pauseTest(60)

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
            assert moonCake.registerInfo.username == displayName
            assert dma.deviceList.plcmDeviceV3List.find { x ->
                x.ipAddress == moonCake.ip && x.plcmSipIdentityV2 != null &&
                        x.plcmSipIdentityV2.sipRegistrationState == SipRegistrationState.ACTIVE
            }.plcmSipIdentityV2.sipUri.contains(mc_sip_username)
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerSip(authName, centralDomain, authPasswd, dma.ip, gsSipProtocol)

        then: "MoonCake place H323 call with the GS"
        moonCake.placeCall(gs_sip_username, CallType.SIP, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()

        where:
        mcSipProtocol | gsSipProtocol
        "TCP"         | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP
        "UCP"         | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_UDP
        "TLS"         | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS
    }
}
