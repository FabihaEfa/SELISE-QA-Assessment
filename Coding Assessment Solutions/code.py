# Coding Assessment Solutions

# 1. Even / Odd
n = int(input("Enter a number: "))
print("Even" if n % 2 == 0 else "Odd")

# 2. Prime check
import math
def is_prime(x):
    if x <= 1:
        return False
    if x <= 3:
        return True
    if x % 2 == 0:
        return False
    for i in range(3, int(math.sqrt(x)) + 1, 2):
        if x % i == 0:
            return False
    return True

num = int(input("Enter number: "))
print("Prime" if is_prime(num) else "Not prime")

# 3. Max in array
arr = list(map(int, input("Enter numbers: ").split()))
print("Max:", max(arr))
