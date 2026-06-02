package controllers

import (
	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"
)

// IsDemoUser extracts the is_demo claim from the JWT token stored in fiber context.
// Returns true if the current request is made by a demo user.
func IsDemoUser(c *fiber.Ctx) bool {
	userToken, ok := c.Locals("user").(*jwt.Token)
	if !ok {
		return false
	}
	claims, ok := userToken.Claims.(jwt.MapClaims)
	if !ok {
		return false
	}
	isDemo, _ := claims["is_demo"].(bool)
	return isDemo
}
