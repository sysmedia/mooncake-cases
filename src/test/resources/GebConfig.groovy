import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.remote.DesiredCapabilities

waiting {
    timeout = 45
}

String downloadPath = System.getProperty("user.dir")+"\\src\\test\\resources\\WebUI\\Download"

environments {
    // run via “./gradlew chromeTest”
    // See: http://code.google.com/p/selenium/wiki/ChromeDriver
    chrome {
        //make change at chrome driver path to keep up with the environment
        //File chromeDriver = new File("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chromedriver.exe")
        //System.setProperty("webdriver.chrome.driver", chromeDriver.getAbsolutePath())
        driver = {
            Map<String, Object> preferences = new Hashtable<String, Object>()
            preferences.put("profile.default_content_settings.popups", 0)
            preferences.put("download.prompt_for_download", "false")
            preferences.put("download.default_directory", downloadPath)

            ChromeOptions options = new ChromeOptions()
            options.addArguments("--start-fullscreen")
            options.addArguments("--lang=en-us")
            options.setExperimentalOption("prefs",preferences)
            def driverInstance = new ChromeDriver(options)
            driverInstance
        }
    }

    // run via “./gradlew firefoxTest”
    // See: http://code.google.com/p/selenium/wiki/FirefoxDriver
    firefox {
        //make change at firefox driver path to keep up with the environment
        //File fireFoxDriver = new File("C:\\Program Files\\Mozilla Firefox\\geckodriver.exe")
        //System.setProperty("webdriver.gecko.driver", fireFoxDriver.getAbsolutePath())
        atCheckWaiting = 5
        FirefoxProfile firefoxProfile = new FirefoxProfile()
        FirefoxOptions options = new FirefoxOptions()
        firefoxProfile.setPreference("browser.download.folderList",2)
        firefoxProfile.setPreference("browser.download.dir", downloadPath)
        firefoxProfile.setPreference("browser.download.useDownloadDir", true)
        firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", "text/csv")
        options.setProfile(firefoxProfile)
        driver = { new FirefoxDriver(options) }
    }

    ie {
        //make change at ie driver path to keep up with the environment
        //File ieDriver = new File("C:\\Users\\ryqi\\Downloads\\IEDriverServer_x64_3.14.0\\IEDriverServer.exe")
        //System.setProperty("webdriver.ie.driver", ieDriver.getAbsolutePath())
        driver = {
            DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer()
            capabilities.setCapability("nativeEvents",false)
            capabilities.setCapability("RequireWindowFocus",true)
            new InternetExplorerDriver(capabilities)
        }
    }

    edge {
        driver = {
            //make change at edge driver path to keep up with the environment
            File edgeDriver = new File("C:\\Users\\ryqi\\Downloads\\MicrosoftWebDriver.exe")
            System.setProperty("webdriver.edge.driver", edgeDriver.getAbsolutePath())
            new EdgeDriver()
        }
    }
}
