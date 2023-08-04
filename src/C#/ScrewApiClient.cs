using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace Screw.Api
{
    public class ScrewApiClient
    {
        private readonly HttpClient httpClient;
        private readonly string apiUrl = "https://api.screwltd.com/";

        public ScrewApiClient()
        {
            httpClient = new HttpClient();
            httpClient.BaseAddress = new Uri(apiUrl);
            httpClient.DefaultRequestHeaders.Accept.Clear();
            httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        }

        public async Task<string> Authenticate(string login, string password)
        {
            var authData = new Dictionary<string, string>
            {
                { "login", login },
                { "password", password }
            };

            var authJson = JsonConvert.SerializeObject(authData);

            var response = await PostRequest("auth", authJson);
            if (!string.IsNullOrEmpty(response))
            {
                var jsonResponse = JsonConvert.DeserializeObject<Dictionary<string, string>>(response);
                return jsonResponse.ContainsKey("access_key") ? jsonResponse["access_key"] : null;
            }

            return null;
        }

        public async Task<bool> CheckAuthValid(string access_key)
        {
            return !string.IsNullOrEmpty(access_key);
        }

        public async Task<string> CreateAppKey(string accessKey)
        {
            var response = await PostRequest("create_app", string.Empty, accessKey);
            if (!string.IsNullOrEmpty(response))
            {
                var jsonResponse = JsonConvert.DeserializeObject<Dictionary<string, string>>(response);
                return jsonResponse.ContainsKey("app_key") ? jsonResponse["app_key"] : null;
            }
            else
            {
                Console.WriteLine($"Failed to create app key. Response is empty.");
                return null;
            }
        }

        public async Task<bool> AuthenticateApp(string appKey, string accessKey)
        {
            var queryParams = $"?app_key={appKey}";
            var response = await PostRequest("auth_app" + queryParams, string.Empty, accessKey);

            if (!string.IsNullOrEmpty(response))
            {
                var jsonResponse = JsonConvert.DeserializeObject<Dictionary<string, object>>(response);
                return jsonResponse.ContainsKey("result") && (bool)jsonResponse["result"];
            }
            return false;
        }

        public async Task<int> CheckAppKeyActivations(string appKey)
        {
            var queryParams = $"?app_key={appKey}";
            var response = await GetRequest("check_app" + queryParams);

            if (!string.IsNullOrEmpty(response))
            {
                var jsonResponse = JsonConvert.DeserializeObject<Dictionary<string, object>>(response);
                return jsonResponse.ContainsKey("activations_left") ? Convert.ToInt32(jsonResponse["activations_left"]) : -1;
            }
            return -1;
        }

        public async Task<bool> UpdateAppKey(string appKey, int numActivations, string accessKey)
        {
            var queryParams = $"?app_key={appKey}&num_activations={numActivations}";
            var response = await PostRequest("update_app_key" + queryParams, string.Empty, accessKey);

            if (!string.IsNullOrEmpty(response))
            {
                var jsonResponse = JsonConvert.DeserializeObject<Dictionary<string, object>>(response);
                return jsonResponse.ContainsKey("message") && jsonResponse["message"].ToString() == "App key activations updated successfully";
            }
            return false;
        }

        private async Task<string> PostRequest(string endpoint, string data, string accessToken = null)
        {
            httpClient.DefaultRequestHeaders.Authorization = accessToken != null ? new AuthenticationHeaderValue("Bearer", accessToken) : null;
            var content = new StringContent(data, Encoding.UTF8, "application/json");
            var response = await httpClient.PostAsync(endpoint, content);

            if (response.IsSuccessStatusCode)
            {
                return await response.Content.ReadAsStringAsync();
            }

            return null;
        }

        private async Task<string> GetRequest(string endpoint)
        {
            var response = await httpClient.GetAsync(endpoint);

            if (response.IsSuccessStatusCode)
            {
                return await response.Content.ReadAsStringAsync();
            }

            return null;
        }
    }
}
