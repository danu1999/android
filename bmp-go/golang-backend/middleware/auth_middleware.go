package middleware

import (
	"invoice-bmp-go/controllers"
	"os"

	jwtware "github.com/gofiber/contrib/jwt"
	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"
)

func Protected() fiber.Handler {
	return jwtware.New(jwtware.Config{
		SigningKey: jwtware.SigningKey{Key: controllers.JwtSecret},

		// 👇 TAMBAHAN OBATNYA DI SINI BOS 👇
		// Biar satpamnya paham kalau tiket dari React ada awalan "Bearer "
		AuthScheme:  "Bearer",
		TokenLookup: "header:Authorization,query:token",

		ErrorHandler: func(c *fiber.Ctx, err error) error {
			return c.Status(fiber.StatusUnauthorized).JSON(fiber.Map{
				"success": false,
				"message": "Unauthorized or token expired",
			})
		},
	})
}

func RestrictDemo() fiber.Handler {
	return func(c *fiber.Ctx) error {
		// Jika server berjalan dalam DEMO_MODE=true (sandbox), izinkan manipulasi data secara bebas
		if os.Getenv("DEMO_MODE") == "true" {
			return c.Next()
		}

		method := c.Method()
		if method == fiber.MethodPost || method == fiber.MethodPut || method == fiber.MethodDelete || method == fiber.MethodPatch {
			userToken, ok := c.Locals("user").(*jwt.Token)
			if ok {
				claims, ok := userToken.Claims.(jwt.MapClaims)
				if ok {
					if isDemo, _ := claims["is_demo"].(bool); isDemo {
						return c.Status(fiber.StatusForbidden).JSON(fiber.Map{
							"success": false,
							"message": "Aksi dinonaktifkan di akun Demo. Anda tidak dapat mengubah data database.",
						})
					}
				}
			}
		}
		return c.Next()
	}
}

// BlockForDemo memblokir SEMUA akses (GET/POST/PUT/DELETE) dari akun Demo
// ke route yang berisi data sensitif produksi (payroll, bahan-nono, kas audit, dll).
func BlockForDemo() fiber.Handler {
	return func(c *fiber.Ctx) error {
		if controllers.IsDemoUser(c) {
			return c.Status(fiber.StatusForbidden).JSON(fiber.Map{
				"success": false,
				"message": "Fitur ini tidak tersedia di akun Demo.",
			})
		}
		return c.Next()
	}
}
