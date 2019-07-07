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
import spock.lang.Unroll

/**
 * Created by taochen on 2019-06-18.
 *
 * Provisioning server: FTP
 * Provision items: call setting call rate--different call rate when oculus as H323 caller.
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
class FTP_Provisioning_Call_Settings_Network_Call_Rate_H323_Caller extends MoonCakeProvisionTestSpec {
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
                'enableSIP'                 : 'disable',
                'e164Number'                : mc_e164Num,
                'h323alias'                 : mc_h323Name,
                'gatekeeperAddress'         : dma.ip,
                'h323AuthEnabled'           : 'false',
                'enableH323'                : 'enable']
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

        then: "Verify if the MoonCake retrieve the settings from the FTP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
            assert moonCake.registerInfo.username == displayName
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

        then: "MoonCake place H323 call with the GS"
        moonCake.placeCall(gs_e164Num, CallType.H323, callRate)

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
