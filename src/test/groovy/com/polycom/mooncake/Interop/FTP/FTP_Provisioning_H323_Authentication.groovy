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
 * Provisioning server: FTPS
 * Provision items: H323 related value
 * <H323>
 * 		<displayName>RealPresence Debut</displayName>               // device name of h323 or sip
 * 		<e164Number>115207</e164Number>                             // h323 number
 * 		<enableH323>enable</enableH323>                             // value: enable\disable
 * 		<gatekeeperAddress>172.21.113.32:2219</gatekeeperAddress>   // gatekeeper address
 * 		<gatekeeperType>specify</gatekeeperType>                    // value: specify
 * 		<h323alias>oculus115207</h323alias>                         // h323 alias
 * 		<h460FireWallTraversal>true</h460FireWallTraversal>         // value: true\false
 * 		<h323AuthEnabled>true</h323AuthEnabled>
 * 		<h323AuthName>debut</h323AuthName>
 * 		<h323AuthPassword>Polycom123</h323AuthPassword>
 * </H323>
 */
class FTP_Provisioning_H323_Authentication extends MoonCakeProvisionTestSpec {
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
    String authName = "mooncake1"

    @Shared
    String authPasswd = "polycom"

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        dma = testContext.bookSut(Dma.class, "Auth")
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

    def "Verify it the MoonCake can be provisioned by the FTPS server with H323 authentication settings"() {
        def attributesToBeSetForFtps = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
                'enableSIP'                 : 'disable',
                'e164Number'                : mc_e164Num,
                'h323alias'                 : mc_h323Name,
                'gatekeeperAddress'         : dma.ip,
                'h323AuthEnabled'           : 'true',
                'enableH323'                : 'enable',
                'h323AuthName'              : authName,
                'h323AuthPassword'          : authPasswd]
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
        retry(times: 3, delay: 10) {
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

        then: "GS register onto the GK server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip, true, authName, authPasswd)

        then: "MoonCake place H323 call with the GS"
        moonCake.placeCall(gs_e164Num, CallType.H323, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
    }
}
