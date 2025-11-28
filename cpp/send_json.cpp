// Simple C++ example that POSTs JSON to the FastAPI backend using libcurl.
// Build (Linux / macOS):
// g++ -std=c++11 send_json.cpp -o send_json -lcurl
//
// Run:
// ./send_json

#include <curl/curl.h>
#include <string>
#include <iostream>

int main() {
    CURL *curl;
    CURLcode res;

    std::string json = R"({"from":"cpp-client","time":"2025-11-28T00:00:00Z","message":"hello from C++"})";
    std::string url = "http://localhost:8000/api/data";

    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl = curl_easy_init();
    if(curl) {
        struct curl_slist *headers = NULL;
        headers = curl_slist_append(headers, "Content-Type: application/json");

        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, json.c_str());
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, (long)json.size());

        res = curl_easy_perform(curl);
        if(res != CURLE_OK) {
            std::cerr << "curl_easy_perform() failed: " << curl_easy_strerror(res) << std::endl;
        } else {
            std::cout << "POST sent successfully\n";
        }

        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    }
    curl_global_cleanup();
    return 0;
}
