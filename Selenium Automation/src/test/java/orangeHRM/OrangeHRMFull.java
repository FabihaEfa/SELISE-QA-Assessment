package orangeHRM;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrangeHRMFull {
    public static void main(String[] args) {
        // 1. Setup ChromeDriver
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        // 2. Navigate to login page
        driver.get("https://opensource-demo.orangehrmlive.com/");

        // 3. Explicit Wait
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            // 4. Login
            WebElement username = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
            username.sendKeys("Admin");
            System.out.println("✅ Entered username");

            WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("password")));
            password.sendKeys("admin123");
            System.out.println("✅ Entered password");

            WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']")));
            loginBtn.click();
            System.out.println("✅ Clicked login button");

            // 5. Wait for Dashboard
            WebElement dashboardHeader = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//h6[text()='Dashboard']"))
            );
            System.out.println("✅ Login successful, Dashboard loaded: " + dashboardHeader.getText());

            // Validate additional dashboard elements
            WebElement adminTab = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[text()='Admin']")));
            System.out.println("✅ Admin tab visible: " + adminTab.isDisplayed());
            WebElement userProfile = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".oxd-userdropdown-name")));
            System.out.println("✅ User profile visible: " + userProfile.isDisplayed());

            // Take screenshot after login
            takeScreenshot(driver, "post_login");

            // 6. Click PIM Tab
            WebElement pimTab = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='PIM']")));
            pimTab.click();
            System.out.println("✅ PIM tab clicked successfully");

            // Take screenshot after PIM tab click
            takeScreenshot(driver, "pim_page");

            // Find column indices dynamically from headers
            List<WebElement> headers = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".oxd-table-header-cell"))
            );
            int firstNameIndex = -1;
            int lastNameIndex = -1;
            int statusIndex = -1;
            for (int i = 0; i < headers.size(); i++) {
                String headerText = headers.get(i).getText().trim();
                if (headerText.equals("First (& Middle) Name")) {
                    firstNameIndex = i + 1;
                } else if (headerText.equals("Last Name")) {
                    lastNameIndex = i + 1;
                } else if (headerText.equals("Employment Status")) {
                    statusIndex = i + 1;
                }
            }

            if (firstNameIndex == -1 || lastNameIndex == -1 || statusIndex == -1) {
                System.out.println("❌ Required columns not found (First (& Middle) Name, Last Name, Employment Status). The table structure may have changed or columns may not be enabled.");
                takeScreenshot(driver, "columns_not_found");
                return;
            }

            System.out.println("✅ Found columns: First Name at nth-child(" + firstNameIndex + "), Last Name at nth-child(" + lastNameIndex + "), Status at nth-child(" + statusIndex + ")");

            // 7. Extract first 5 employee names and statuses
            List<WebElement> employeeRows = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(
                            By.cssSelector(".oxd-table-row.oxd-table-row--with-border.oxd-table-row--clickable")
                    )
            );

            if (employeeRows.isEmpty()) {
                System.out.println("❌ No employee rows found. Check table CSS selector or page content.");
                takeScreenshot(driver, "no_employee_rows");
                return;
            }

            System.out.println("Found " + employeeRows.size() + " employee rows");
            boolean statusFound = false;
            System.out.println("First 5 employees with their status:");

            for (int i = 0; i < Math.min(5, employeeRows.size()); i++) {
                try {
                    WebElement row = employeeRows.get(i);

                    String firstName = row.findElement(By.cssSelector("div.oxd-table-cell:nth-child(" + firstNameIndex + ") > div")).getText().trim();
                    String lastName = row.findElement(By.cssSelector("div.oxd-table-cell:nth-child(" + lastNameIndex + ") > div")).getText().trim();
                    String status = row.findElement(By.cssSelector("div.oxd-table-cell:nth-child(" + statusIndex + ") > div")).getText().trim();

                    String fullName = firstName + " " + lastName;
                    System.out.println((i + 1) + ". " + fullName + " | Status: " + status);

                    // Validation
                    if (fullName.trim().isEmpty()) {
                        System.out.println("❌ Error: Employee name is empty at row " + (i + 1));
                        takeScreenshot(driver, "empty_name_row_" + (i + 1));
                    }

                    if (status.equalsIgnoreCase("Full-Time Permanent")) {
                        statusFound = true;
                    }
                } catch (Exception e) {
                    System.out.println("❌ Error processing row " + (i + 1) + ": " + e.getMessage());
                    takeScreenshot(driver, "error_row_" + (i + 1));
                }
            }

            // Check if at least one Full-Time Permanent exists
            if (statusFound) {
                System.out.println("✅ At least one 'Full-Time Permanent' employee found");
            } else {
                System.out.println("❌ No 'Full-Time Permanent' employee found");
                takeScreenshot(driver, "no_full_time_status");
            }

        } catch (Exception e) {
            System.out.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            takeScreenshot(driver, "error_general");
        } finally {
            // 8. Close browser
            driver.quit();
        }
    }

    private static void takeScreenshot(WebDriver driver, String fileName) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File destination = new File("screenshots/" + fileName + "_" + timestamp + ".png");
            FileUtils.copyFile(screenshot, destination);
            System.out.println("📸 Screenshot saved: " + destination.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("❌ Failed to save screenshot: " + e.getMessage());
        }
    }
}