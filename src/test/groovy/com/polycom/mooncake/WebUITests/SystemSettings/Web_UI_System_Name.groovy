package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.GeneralPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by qxu on 5/22/2019
 */
class Web_UI_System_Name extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Name.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("General")
    }

    @Shared
    def defaultName = "MoonCake"
    @Shared
    def name = "Test-RPDebut_ABC.longname1234567890lskdjhtowuerlkjdshgj"
    @Shared
    def actName = "Test-RPDebut_ABC.longname1234567890lskdjhtowuerl"  //max length support is 48chars
    def "set MoonCake name to #name"(){
        when: "Open General page"
        GeneralPage generalPage = browser.at GeneralPage

        and: "set System Name and submit"
        generalPage.setSystemName(name)
        generalPage.clickSubmitButton()
        pauseTest(2)

        then: "check System Name"
        generalPage.getSystemName().value() == actName

        then: "set back to default name"
        generalPage.setSystemName(defaultName)
        generalPage.clickSubmitButton()
        pauseTest(2)

        then: "check System Name change back to default"
        generalPage.getSystemName().value() == defaultName
    }
}
