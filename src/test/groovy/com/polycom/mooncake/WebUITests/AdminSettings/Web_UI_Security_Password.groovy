package com.polycom.mooncake.WebUITests.AdminSettings

import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.PasswordPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.LoginPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by qxu on 5/24/2019
 */
class Web_UI_Security_Password extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Security_Password.class)

    @Shared
    def userName = "admin"
    @Shared
    def oldPwd = ""
    @Shared
    def newPwd1 = "12"
    @Shared
    def newPwd2 = "123"

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("Password")
    }

    def "verify MoonCake Web UI password reset function"(){
        when: "open password page"
        PasswordPage passwordPage = browser.at PasswordPage

        and: "change the admin password by entering two different new passwords"
        passwordPage.changePassword(oldPwd, newPwd1, newPwd2)
        pauseTest(3)

        then:"change failed, still stay on password page"
        assert browser.at(PasswordPage)

        then: "change the admin password by entering correct new passwords"
        passwordPage.changePassword(oldPwd, newPwd2, newPwd2)
        pauseTest(3)

        then: "change success, page go to login page"
        LoginPage loginPage =  browser.at LoginPage

        then: "re-login to web page with new admin password"
        loginPage.loginWeb(userName, newPwd2)
        pauseTest(3)

        then: "login to device status page"
        DeviceStatusPage deviceStatusPage = browser.at  DeviceStatusPage

        then: "switch to password page and change the password back to original one"
        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("Password")
        at(PasswordPage)
        passwordPage.changePassword(newPwd2, oldPwd, oldPwd)

        then: "change success and switch to device status page"
        assert browser.at(DeviceStatusPage)
    }
}
