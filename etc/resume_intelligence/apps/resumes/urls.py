from django.urls import path

from . import views

app_name = "resumes"

urlpatterns = [
    path("", views.dashboard_view, name="dashboard"),
    path("upload/", views.upload_view, name="upload"),
    path("<uuid:resume_id>/", views.detail_view, name="detail"),
    path("<uuid:resume_id>/delete/", views.delete_view, name="delete"),
    path("<uuid:resume_id>/confirm/", views.confirm_view, name="confirm"),
    path("<uuid:resume_id>/reanalyze/", views.reanalyze_view, name="reanalyze"),
    path("<uuid:resume_id>/pdf/", views.pdf_view, name="pdf"),
    path("<uuid:resume_id>/excel/", views.excel_view, name="excel"),
    path("<uuid:resume_id>/raw-text/", views.raw_text_view, name="raw_text"),
    path("<uuid:resume_id>/basic/", views.basic_info_edit_view, name="basic_edit"),
    path("<uuid:resume_id>/careers/add/", views.career_add_view, name="career_add"),
    path("<uuid:resume_id>/careers/<int:index>/delete/", views.career_delete_view, name="career_delete"),
    path("<uuid:resume_id>/educations/add/", views.education_add_view, name="education_add"),
    path("<uuid:resume_id>/educations/<int:index>/delete/", views.education_delete_view, name="education_delete"),
]
