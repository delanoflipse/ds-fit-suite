package proxy

import (
	"net/http"
	"net/http/httputil"

	"dflipse.nl/fit-proxy/faultload"
	"dflipse.nl/fit-proxy/tracing"
)

type ProxyState struct {
	InjectedFault      *faultload.Fault
	ConcurrentFaults   []*faultload.FaultUid
	Proxy              *httputil.ReverseProxy
	ResponseWriter     *ResponseCapture
	Request            *http.Request
	ReponseOverwritten bool
	Complete           bool
}

func (s ProxyState) asReport(metadata tracing.RequestMetadata, hashBody bool) tracing.RequestReport {
	var response *tracing.ResponseData = nil

	if s.Complete {
		responseData := s.ResponseWriter.GetResponseData(hashBody)
		response = &responseData
	}

	return tracing.RequestReport{
		TraceId:       metadata.TraceId,
		ParentId:      metadata.ParentId,
		SpanId:        metadata.SpanId,
		FaultUid:      metadata.FaultUid,
		IsInitial:     metadata.IsInitial,
		InjectedFault: s.InjectedFault,
		Response:      response,
		ConcurrentTo:  s.ConcurrentFaults,
	}
}
