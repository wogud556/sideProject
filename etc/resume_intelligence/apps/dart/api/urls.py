from django.urls import path

from . import views

app_name = "dart_api"

urlpatterns = [
    path("dart/corporations/search", views.corporation_search_view, name="corporation_search"),
    path("dart/corporations/<str:corp_code>", views.corporation_detail_view, name="corporation_detail"),
    path(
        "dart/corporations/<str:corp_code>/financials",
        views.corporation_financials_view,
        name="corporation_financials",
    ),
    path(
        "dart/corporations/<str:corp_code>/refresh",
        views.corporation_refresh_view,
        name="corporation_refresh",
    ),
    path(
        "resumes/<uuid:resume_id>/careers/<int:index>/dart-company",
        views.career_dart_company_view,
        name="career_dart_company",
    ),
]
