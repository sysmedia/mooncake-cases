package com.polycom.mooncake.Interop.FTP


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
import spock.lang.Unroll

/**
 * Created by taochen on 2019-06-17.
 *
 * Provisioning server: FTP
 * Provision items: call setting call rate--different call rate when MoonCake as sip callee.
 * <CallSettings>
 * 		<aesEcription>auto</aesEcription>       // value: auto\disable\enable
 * 		<natConfig>off</natConfig>              // value: on\off
 * 		<natWANAddress></natWANAddress>         // nat WAN Address
 * 		<networkCallRate>4096</networkCallRate> // value: 64\256\384\512\768\1024\1536\2048\3072\4096
 * 		<tcpStartPort>3230</tcpStartPort>       // value: defaut 3030
 * 		<tcpEndPort>3250</tcpEndPort>           // value: tcpStartPort + 20
 * 		<udpStartPort>3230</udpStartPort>       // value: defaut 3030
 * 		<udpEndPort>3250</udpEndPort>           // value: udpStartPort + 20
 * 		<useFixedPorts>false</useFixedPorts>    // value: true\false
 * </CallSettings>
 */
class FTP_Provisioning_Call_Settings_Network_Call_Rate_SIP_Callee extends MoonCakeProvisionTestSpec {
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
    def "Verify if the MoonCake can be set the call rate with the FTP provision and can call with the remote endpoint with rate #callRate KBPS"(int callRate) {
        def attributesToBeSet = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'displayName'               : 'Poly Group 150',
                'networkCallRate'           : String.valueOf(callRate),
                'enableSIP'                 : 'enable',
                'sipProxyServer'            : dma.ip,
                'sipRegistrarServer'        : dma.ip,
                'userName'                  : mc_sip_username,
                'transport'                 : 'TCP',
                'enableH323'                : 'disable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSet)
        pauseTest(5)

        then: "Set the MoonCake provision as FTP"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)
        pauseTest(60)

        then: "Update the MoonCake call rate"
        moonCake.updateCallSettings(4096, "off", true, false, true)

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
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

        then: "GS place SIP call with the MoonCake"
        groupSeries.placeCall(mc_sip_username, CallType.SIP, callRate)

        then: "Verify the current call rate"
        moonCake.mediaStatistics.callRate == callRate

        then: "Verify the media statistics during the call"
        if (callRate != 64) {
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()

        where:
        callRate | _
        64       | _
        256      | _
        384      | _
        512      | _
        768      | _
        1024     | _
        1536     | _
        2048     | _
        3072     | _
        4096     | _
    }
}
