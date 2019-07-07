package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.NetworkSettingsPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import geb.driver.CachingDriverFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/10/2019
 */
class Web_UI_Network_IP extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Network_IP.class)

    @Shared
    String IP

    @Shared
    String subnetMask = "255.255.255.0"

    @Shared
    String gateWay

    @Shared
    String DNS = "172.21.6.30"

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("NetworkSetting")
        IP = moonCake.getIp()
        def firstThreeOctetcs = IP.substring(0, IP.lastIndexOf("."))
        gateWay = firstThreeOctetcs + ".254"
    }

    def "Configure network to Static IP mode"() {
        when: "Open network setting page"
        NetworkSettingsPage networkSettingsPage = browser.at NetworkSettingsPage

        and: "Set network as static mode"
        networkSettingsPage.getDhcpMode().selected = "number:1"

        then: "Check value"
        networkSettingsPage.getDhcpMode().selectedText == "Static IP"

        when: "Set network configuration"
        networkSettingsPage.getIpAddress().value(IP)
        networkSettingsPage.getSubnetMask().value(subnetMask)
        networkSettingsPage.getGateway().value(gateWay)
        networkSettingsPage.getPreferredDNS().value(DNS)

        then: "Save configuration"
        networkSettingsPage.getSubmitButton().click()

        cleanup:
        resetBrowser()
        CachingDriverFactory.clearCacheAndQuitDriver()
    }

    def "Configure network to DHCP mode again"() {
        setup:
        browser.driver.manage().window().maximize()
        DeviceStatusPage deviceStatusPage = browser.via DeviceStatusPage
        if (browser.driver.toString().contains("InternetExplorerDriver")) {
            browser.driver.get("javascript:document.getElementById('overridelink').click()")
        }

        when: "Open network setting page"
        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("NetworkSetting")
        NetworkSettingsPage networkSettingsPage = browser.at NetworkSettingsPage

        and: "Set network as static mode"
        networkSettingsPage.getDhcpMode().selected = "number:0"

        then: "Check value"
        networkSettingsPage.getDhcpMode().selectedText == "DHCP"
        //networkSettingsPage.getSubmitButton().click()
    }

}
