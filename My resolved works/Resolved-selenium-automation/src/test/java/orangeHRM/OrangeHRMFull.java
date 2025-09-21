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
import java.util.ArrayList;
import java.util.List;

public class OrangeHRMFull {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.get("https://opensource-demo.orangehrmlive.com/");

        try {
            // Login
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username"))).sendKeys("Admin");
            System.out.println("✅ Entered username");

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("password"))).sendKeys("admin123");
            System.out.println("✅ Entered password");

            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))).click();
            System.out.println("✅ Clicked login button");

            WebElement dashboardHeader = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//h6[text()='Dashboard']")));
            System.out.println("✅ Login successful, Dashboard loaded: " + dashboardHeader.getText());

            takeScreenshot(driver, "post_login");

            // Navigate to PIM
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='PIM']"))).click();
            System.out.println("✅ PIM tab clicked successfully");

            takeScreenshot(driver, "pim_page");

            // Find column headers
            List<WebElement> headers = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".oxd-table-header-cell")));

            int firstNameIndex = -1, lastNameIndex = -1, statusIndex = -1;
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i).getText().trim();
                if (header.equals("First (& Middle) Name")) firstNameIndex = i + 1;
                else if (header.equals("Last Name")) lastNameIndex = i + 1;
                else if (header.equals("Employment Status")) statusIndex = i + 1;
            }

            if (firstNameIndex == -1 || lastNameIndex == -1 || statusIndex == -1) {
                System.out.println("❌ Column indices not found.");
                takeScreenshot(driver, "columns_not_found");
                return;
            }

            System.out.println("✅ Found columns: First Name at " + firstNameIndex +
                    ", Last Name at " + lastNameIndex +
                    ", Status at " + statusIndex);

            // 🔄 Collect employees from ALL pages
            List<String[]> allEmployees = new ArrayList<>();
            boolean hasNext = true;

            while (hasNext) {
                List<WebElement> employeeRows = wait.until(
                        ExpectedConditions.visibilityOfAllElementsLocatedBy(
                                By.cssSelector(".oxd-table-row.oxd-table-row--with-border.oxd-table-row--clickable")
                        )
                );

                for (WebElement row : employeeRows) {
                    String firstName = row.findElement(By.cssSelector("div.oxd-table-cell:nth-child(" + firstNameIndex + ") > div")).getText().trim();
                    String lastName = row.findElement(By.cssSelector("div.oxd-table-cell:nth-child(" + lastNameIndex + ") > div")).getText().trim();
                    String status = row.findElement(By.cssSelector("div.oxd-table-cell:nth-child(" + statusIndex + ") > div")).getText().trim();
                    allEmployees.add(new String[]{firstName + " " + lastName, status});
                }

                // Check if "Next" is enabled
                List<WebElement> nextButtons = driver.findElements(By.xpath("//button[@class='oxd-pagination-page-item oxd-pagination-next']"));
                if (!nextButtons.isEmpty() && nextButtons.get(0).isEnabled()) {
                    nextButtons.get(0).click();
                    Thread.sleep(2000); // wait for next page to load
                } else {
                    hasNext = false;
                }
            }

            System.out.println("📊 Total employees collected: " + allEmployees.size());

            // ✅ Pick first 5 ensuring at least one "Full-Time Permanent"
            List<String[]> finalList = new ArrayList<>();
            boolean fullTimeFound = false;

            // First, try to find one Full-Time Permanent
            for (String[] emp : allEmployees) {
                if (emp[1].equalsIgnoreCase("Full-Time Permanent")) {
                    finalList.add(emp);
                    fullTimeFound = true;
                    break;
                }
            }

            // Fill remaining slots (max 5)
            for (String[] emp : allEmployees) {
                if (finalList.size() >= 5) break;
                if (!finalList.contains(emp)) {
                    finalList.add(emp);
                }
            }

            // Print results
            System.out.println("First 5 employees with their status (ensuring at least 1 Full-Time Permanent if exists):");
            int count = 1;
            for (String[] emp : finalList) {
                System.out.println(count + ". " + emp[0] + " | Status: " + emp[1]);
                count++;
            }

            if (fullTimeFound) {
                System.out.println("✅ At least one 'Full-Time Permanent' employee found");
            } else {
                System.out.println("❌ No 'Full-Time Permanent' employee found in all pages");
                takeScreenshot(driver, "no_full_time_status");
            }

        } catch (Exception e) {
            System.out.println("❌ Test failed: " + e.getMessage());
            takeScreenshot(driver, "error_general");
        } finally {
            driver.quit();
        }
    }

    private static void takeScreenshot(WebDriver driver, String fileName) {
        try {
            // Wait for 2 seconds to ensure page fully loads and spinner disappears
            Thread.sleep(2000);

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File destination = new File("screenshots/" + fileName + "_" + timestamp + ".png");
            FileUtils.copyFile(screenshot, destination);
            System.out.println("📸 Screenshot saved: " + destination.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("❌ Screenshot failed: " + e.getMessage());
        }
    }
}
