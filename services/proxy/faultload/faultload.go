package faultload

import (
	"encoding/json"
	"log"
	"net/http"
)

type Faultload struct {
	Faults  []Fault `json:"faults"`
	TraceId string  `json:"trace_id"`
}

func ParseRequest(r *http.Request) (*Faultload, error) {
	var faultload Faultload

	err := json.NewDecoder(r.Body).Decode(&faultload)

	if err != nil {
		log.Printf("Failed to decode request body: %v\n", err)
		return nil, err
	}

	return &faultload, nil
}
