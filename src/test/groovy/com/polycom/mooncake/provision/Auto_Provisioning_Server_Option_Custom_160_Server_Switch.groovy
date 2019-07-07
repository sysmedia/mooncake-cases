package com.polycom.mooncake.provision

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
 * Created by taochen on 2019-06-11.
 *
 * Provisioning mode: DHCP auto_160
 * Manually swith to other provision server after DHCP 160 provisioning.
 */
class Auto_Provisioning_Server_Option_Custom_160_Server_Switch extends MoonCakeProvisionTestSpec {
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

    def "Verify if the MoonCake can be provisioned by the DHCP option 160 and can be switched to FTP provision mode"() {
        def attributesToBeSetForFtps = [provision: [
                'enableSIP'         : 'enable',
                'sipProxyServer'    : dma.ip,
                'sipRegistrarServer': dma.ip,
                'userName'          : mc_sip_username,
                'transport'         : 'TCP',
                'enableH323'        : 'disable']
        ]

        def attributesToBeSetForFtp = [provision: [
                'enableSIP'        : 'disable',
                'e164Number'       : mc_e164Num,
                'h323alias'        : mc_h323Name,
                'gatekeeperAddress': dma.ip,
                'h323AuthEnabled'  : 'false',
                'enableH323'       : 'enable']
        ]

        when: "Create the configuration on the FTP server so that the MoonCake can retrieve the settings"
        FtpClient ftpClient = new FtpClient(FTP_SERVER, FTP_USER, FTP_PASSWORD, "TLS")
        FtpClient ftpsClient = new FtpClient(FTP_SERVER, FTPS_USER, FTPS_PASSWORD, "TLS")
        deleteConfigOnFtp(ftpsClient, mac)
        createConfigOnFtp(ftpsClient, mac)
        deleteConfigOnFtp(ftpClient, mac)
        createConfigOnFtp(ftpClient, mac)

        then: "Update the DHCH settings for later provisioning"
        doCommandOnHttp(DHCP_SERVER, cmdDel66)
        doCommandOnHttp(DHCP_SERVER, cmdDel160)
        doCommandOnHttp(DHCP_SERVER, cmdFtpsSet160)

        then: "Update the configuration on the FTP server so that the MoonCake can retrieve SIP/H323 settings"
        modifyConfigOnFtp(ftpsClient, mac, attributesToBeSetForFtps)
        modifyConfigOnFtp(ftpClient, mac, attributesToBeSetForFtp)
        pauseTest(5)

        then: "MoonCake provision onto the DHCP server to retrieve the settings on option 66"
        moonCake.updateProvisioningSettings(ProvisioningMode.AUTO_CUSTOMER, 160, "", "", "", "")

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.sipStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredSipServerAddress == dma.ip
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

        then: "MoonCake place SIP call with the GS"
        moonCake.placeCall(groupSeries, CallType.SIP, 2048)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")

        then: "Hang up the call for next provisioning"
        moonCake.hangUp()

        then: "Switch MoonCake provision mode to the FTP server to retrieve the settings"
        moonCake.updateProvisioningSettings(ProvisioningMode.MANUAL_FTP, 66, FTP_SERVER, "", FTP_USER, FTP_PASSWORD)

        then: "Verify if the MoonCake retrieve the settings from the DHCP Server"
        retry(times: 3, delay: 5) {
            assert moonCake.gkStatus == ServiceStatus.CONNECTED
            assert moonCake.registeredGkServerAddress == dma.ip
        }

        when: "Provision the GS onto the SIP server"
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)

        then: "MoonCake place SIP call with the GS"
        moonCake.placeCall(groupSeries, CallType.H323, 2048)

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
