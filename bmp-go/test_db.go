package main

import (
	"fmt"
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/google/uuid"
	"github.com/joho/godotenv"
)

func main() {
	godotenv.Load("golang-backend/.env")
	database.Connect()

	product := models.MasterProduct{
		Title:       "Test Product",
		Unit:        "Pcs",
		Price:       1000,
		BeratGram:   10,
		CycleTime:   5,
		DateCreated: time.Now(),
		LastUpdated: time.Now(),
		UniqueID:    uuid.New().String()[:8],
	}

	if err := database.DB.Create(&product).Error; err != nil {
		fmt.Println("Error:", err)
	} else {
		fmt.Println("Success")
	}
}
