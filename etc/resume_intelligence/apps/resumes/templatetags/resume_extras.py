"""detail.html에서 sub_projects 중 검토필요 항목 존재 여부를 확인하기 위한 필터."""
from django import template

register = template.Library()


@register.filter
def any_review_required(items):
    """items(리스트) 중 review_required=True인 항목이 하나라도 있으면 True."""
    return any(getattr(item, "review_required", False) for item in items or [])
