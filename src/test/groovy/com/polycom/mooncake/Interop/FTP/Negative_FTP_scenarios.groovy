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

/**
 * Created by taochen on 2019-06-22.
 *
 * Provisioning server: FTP/FTPS
 * Provision negative scenarios
 * 1. FQDN not resolve.
 * 2. Server not accessible.
 * 3. User name or password incorrect.
 * 4. Protocol incorrect.
 */
class Negative_FTP_scenarios extends MoonCakeProvisionTestSpec {
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

    def "Verify MoonCake can correctly handle the negative FTP provision scenarios"() {
        def attributesToBeSetForFtp = [provision: [
                'profileUpdateCheckInterval': 'PT30S',
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
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSetForFtp)
        pauseTest(5)

        then: "Set the MoonCake provision as FTP with wrong FQDN"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, "ftp.b.com", "", FTP_USER, FTP_PASSWORD)
        pauseTest(5)

        then: "Verify if the MoonCake cannot be provisioned wrong FTP"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.DISCONNECTED || moonCake.sipStatus == ServiceStatus.UNKNOWN
        }

        when: "Set the MoonCake provision as FTP with wrong IP address"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, "1.1.1.1", "", FTP_USER, FTP_PASSWORD)
        pauseTest(5)

        then: "Verify if the MoonCake cannot be provisioned wrong FTP"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.DISCONNECTED || moonCake.sipStatus == ServiceStatus.UNKNOWN
        }

        when: "Set the MoonCake provision as FTP with wrong FTP account"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, "wrongPwd")
        pauseTest(5)

        then: "Verify if the MoonCake cannot be provisioned wrong FTP"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.DISCONNECTED || moonCake.sipStatus == ServiceStatus.UNKNOWN
        }

        when: "Set the MoonCake provision as FTP with wrong protocol"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTPS, 66, FTP_SERVER, "", FTP_USER, "wrongPwd")
        pauseTest(5)

        then: "Verify if the MoonCake cannot be provisioned wrong FTP"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.DISCONNECTED || moonCake.sipStatus == ServiceStatus.UNKNOWN
        }

        when: "Set the MoonCake provision as FTP with correct information"
        moonCake.updateProvisioningSettings(ProvisioningMode.DISABLE, 66, "", "", "", "")
        pauseTest(5)

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

        when: "Provision the GS onto the SIP server"
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

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
    }
}
