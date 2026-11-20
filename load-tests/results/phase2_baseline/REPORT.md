# Kết quả load test Pha 2: `BASELINE`

- Máy: Kaggle CPU, Intel(R) Xeon(R) CPU @ 2.20GHz, 4 CPU logic, 31.35 GB RAM
- Dữ liệu: 100 005 chuyến xe, 284 940 booking
- Mỗi mức tải: 3 lượt x 60 giây, lấy trung vị

## Bảng số liệu

| Kịch bản | Người dùng ảo | Request | RPS | p50 (ms) | p95 (ms) | p99 (ms) | Lỗi (%) |
|---|---|---|---|---|---|---|---|
| read_unfiltered | 1 | GET /api/trips | 0,4 | 1 800 | 1 900 | 2 000 | 0,00 |
| read_unfiltered | 1 | GET /api/trips/{id} | 0,2 | 9 | 13 | 13 | 0,00 |
| read_unfiltered | 1 | **Tổng** | 0,6 | 1 700 | 1 900 | 2 000 | 0,00 |
| read_unfiltered | 5 | GET /api/trips | 0,7 | 7 100 | 7 800 | 8 000 | 0,00 |
| read_unfiltered | 5 | GET /api/trips/{id} | 0,2 | 22 | 76 | 76 | 0,00 |
| read_unfiltered | 5 | **Tổng** | 0,8 | 7 000 | 7 800 | 8 000 | 0,00 |
| read_search | 20 | GET /api/trips/{id} | 11,3 | 4 | 7 | 12 | 0,00 |
| read_search | 20 | GET /api/trips?origin&destination&date | 34,9 | 21 | 35 | 50 | 0,00 |
| read_search | 20 | **Tổng** | 46,7 | 18 | 33 | 47 | 0,00 |
| read_search | 100 | GET /api/trips/{id} | 36,6 | 150 | 250 | 300 | 0,00 |
| read_search | 100 | GET /api/trips?origin&destination&date | 110,4 | 220 | 330 | 390 | 0,00 |
| read_search | 100 | **Tổng** | 146,4 | 200 | 320 | 380 | 0,00 |
| hot_trip | 20 | GET /api/trips/{id} (during hot trip) | 9,6 | 3 | 4 | 7 | 0,00 |
| hot_trip | 20 | POST /api/bookings (hot trip) | 38,0 | 10 | 16 | 22 | 0,00 |
| hot_trip | 20 | **Tổng** | 47,7 | 10 | 15 | 21 | 0,00 |
| hot_trip | 100 | GET /api/trips/{id} (during hot trip) | 45,7 | 4 | 14 | 27 | 0,00 |
| hot_trip | 100 | POST /api/bookings (hot trip) | 173,6 | 17 | 78 | 120 | 0,00 |
| hot_trip | 100 | **Tổng** | 219,2 | 14 | 70 | 120 | 0,00 |
| hot_trip | 400 | GET /api/trips/{id} (during hot trip) | 70,7 | 690 | 1 100 | 1 300 | 0,00 |
| hot_trip | 400 | POST /api/bookings (hot trip) | 202,5 | 1 100 | 1 600 | 1 900 | 0,00 |
| hot_trip | 400 | **Tổng** | 273,2 | 1 100 | 1 600 | 1 800 | 0,00 |
| register_smtp_ok | 20 | POST /api/auth/register | 29,9 | 250 | 400 | 460 | 0,00 |
| register_smtp_ok | 20 | **Tổng** | 29,9 | 250 | 400 | 460 | 0,00 |
| register_smtp_hang | 20 | POST /api/auth/register | 3,4 | 5 100 | 5 100 | 5 200 | 100,00 |
| register_smtp_hang | 20 | **Tổng** | 3,4 | 5 100 | 5 100 | 5 200 | 100,00 |

## Tài nguyên và tranh chấp (dòng tổng của mỗi kịch bản)

| Kịch bản | Người dùng ảo | CPU Java (%) | RAM Java tối đa (MB) | Request chờ kết nối DB (tối đa) | Chờ khóa trung bình (ms) | Số lượt backend chết |
|---|---|---|---|---|---|---|
| read_unfiltered | 1 | 117,0 | 1 212 | 0 | - | 0 |
| read_unfiltered | 5 | 290,3 | 1 413 | 0 | - | 0 |
| read_search | 20 | 26,9 | 657 | 0 | - | 0 |
| read_search | 100 | 24,8 | 658 | 18 | - | 0 |
| hot_trip | 20 | 25,0 | 722 | 0 | 2,38 | 0 |
| hot_trip | 100 | 61,3 | 735 | 7 | 16,45 | 0 |
| hot_trip | 400 | 72,6 | 772 | 190 | 43,20 | 0 |
| register_smtp_ok | 20 | 334,8 | 797 | 0 | - | 0 |
| register_smtp_hang | 20 | 33,0 | 798 | 0 | - | 0 |

## Kiểm tra tính đúng

- Chuyến "hot": 80243 booking, 0 chuyến lệch số ghế.
- Đăng ký: 6442 tài khoản được tạo và còn chờ xác minh (gồm cả các lượt đăng ký bị lỗi khi SMTP treo).
- Backend khởi động 1 lần; 0 lỗi hết bộ nhớ trong log.

CPU Java là tổng trên mọi nhân (100% = một nhân). Backend, PostgreSQL và Locust chạy chung một máy.
Giới hạn đăng ký được nâng lên 1000000 request/phút để đo kịch bản đăng ký.
